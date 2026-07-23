<?php
/**
 * Setup Web Service Permissions for Chatbot
 * 
 * This script will:
 * 1. Enable web services
 * 2. Enable REST protocol
 * 3. Create/update external service with required functions
 * 4. Assign Manager role to webservice user
 * 5. Enable required capabilities
 * 
 * Usage: php setup_webservice_permissions.php
 * 
 * @package    local_chatbot
 * @author     Nguyễn Đình Nhật Huy - MSSV: 110122223
 */

define('CLI_SCRIPT', true);

require(__DIR__.'/../../config.php');
require_once($CFG->libdir.'/clilib.php');
require_once($CFG->dirroot.'/webservice/lib.php');

// Ensure errors are well explained.
$CFG->debug = DEBUG_DEVELOPER;
$CFG->debugdisplay = 1;

// CLI only.
if (!CLI_SCRIPT) {
    die('This script must be run from command line');
}

echo "=====================================\n";
echo "🤖 CHATBOT WEB SERVICE SETUP\n";
echo "=====================================\n\n";

// Step 1: Enable web services
echo "Step 1: Enabling web services...\n";
set_config('enablewebservices', 1);
echo "✅ Web services enabled\n\n";

// Step 2: Enable REST protocol
echo "Step 2: Enabling REST protocol...\n";
set_config('webserviceprotocols', 'rest');
echo "✅ REST protocol enabled\n\n";

// Step 3: Find or create webservice user
echo "Step 3: Checking webservice user...\n";
$wsuser = $DB->get_record('user', array('username' => 'webservice'));
if (!$wsuser) {
    echo "⚠️  Webservice user not found. Creating...\n";
    $wsuser = new stdClass();
    $wsuser->username = 'webservice';
    $wsuser->password = password_hash('Webservice@2026', PASSWORD_BCRYPT);
    $wsuser->firstname = 'Web';
    $wsuser->lastname = 'Service';
    $wsuser->email = 'webservice@example.com';
    $wsuser->auth = 'manual';
    $wsuser->confirmed = 1;
    $wsuser->mnethostid = $CFG->mnet_localhost_id;
    $wsuser->timecreated = time();
    $wsuser->timemodified = time();
    $wsuser->id = $DB->insert_record('user', $wsuser);
}
echo "✅ Webservice user found: ID={$wsuser->id}, Username={$wsuser->username}\n\n";

// Step 4: Assign Manager role to webservice user
echo "Step 4: Assigning Manager role to webservice user...\n";
$managerroleid = $DB->get_field('role', 'id', array('shortname' => 'manager'));
if (!$managerroleid) {
    echo "❌ Manager role not found!\n";
    exit(1);
}

$context = context_system::instance();
$existingrole = $DB->get_record('role_assignments', array(
    'roleid' => $managerroleid,
    'contextid' => $context->id,
    'userid' => $wsuser->id
));

if (!$existingrole) {
    role_assign($managerroleid, $wsuser->id, $context->id);
    echo "✅ Manager role assigned to webservice user\n";
} else {
    echo "✅ Manager role already assigned\n";
}
echo "\n";

// Step 5: Create or update external service
echo "Step 5: Setting up external service...\n";
$servicename = 'Chatbot Full Service';
$serviceshortname = 'chatbot_full';

$service = $DB->get_record('external_services', array('shortname' => $serviceshortname));
if (!$service) {
    echo "Creating new service: $servicename\n";
    $service = new stdClass();
    $service->name = $servicename;
    $service->shortname = $serviceshortname;
    $service->enabled = 1;
    $service->restrictedusers = 0;
    $service->downloadfiles = 1;
    $service->uploadfiles = 1;
    $service->timecreated = time();
    $service->timemodified = time();
    $service->id = $DB->insert_record('external_services', $service);
    echo "✅ Service created: ID={$service->id}\n";
} else {
    echo "✅ Service already exists: ID={$service->id}\n";
    // Update to ensure it's enabled
    $service->enabled = 1;
    $service->restrictedusers = 0;
    $service->timemodified = time();
    $DB->update_record('external_services', $service);
    echo "✅ Service updated\n";
}
echo "\n";

// Step 6: Add required functions to service
echo "Step 6: Adding required functions to service...\n";
$functions = array(
    'core_webservice_get_site_info',
    'core_course_get_courses',
    'core_enrol_get_enrolled_users',
    'core_user_get_users_by_field',
    'gradereport_user_get_grade_items',
    'core_course_get_contents',
    'core_user_get_course_user_profiles',
    'core_grades_get_grades',
    'core_course_get_categories',
    'core_cohort_get_cohorts',
);

$added = 0;
$skipped = 0;

foreach ($functions as $functionname) {
    // Check if function exists in Moodle
    if (!$DB->record_exists('external_functions', array('name' => $functionname))) {
        echo "⚠️  Function not found in Moodle: $functionname (skipping)\n";
        continue;
    }
    
    // Check if already added
    $exists = $DB->get_record('external_services_functions', array(
        'externalserviceid' => $service->id,
        'functionname' => $functionname
    ));
    
    if (!$exists) {
        $record = new stdClass();
        $record->externalserviceid = $service->id;
        $record->functionname = $functionname;
        $DB->insert_record('external_services_functions', $record);
        echo "  ✅ Added: $functionname\n";
        $added++;
    } else {
        $skipped++;
    }
}

echo "\n✅ Functions added: $added, already exists: $skipped\n\n";

// Step 7: Find existing token or create new one
echo "Step 7: Setting up token...\n";
$tokenstring = '750c631dcc93dfcb6febf36dd4df76f2';

// Check if token exists
$token = $DB->get_record('external_tokens', array('token' => $tokenstring));

if ($token) {
    echo "✅ Token found: $tokenstring\n";
    // Update token to use our service
    $token->externalserviceid = $service->id;
    $token->validuntil = 0; // No expiry
    $token->iid = null;
    $token->sid = null;
    $token->contextid = context_system::instance()->id;
    $token->creatorid = $wsuser->id;
    $token->timemodified = time();
    $DB->update_record('external_tokens', $token);
    echo "✅ Token updated to use service: $servicename\n";
} else {
    echo "⚠️  Token not found. Creating new token...\n";
    $token = new stdClass();
    $token->token = $tokenstring;
    $token->userid = $wsuser->id;
    $token->tokentype = EXTERNAL_TOKEN_PERMANENT;
    $token->externalserviceid = $service->id;
    $token->contextid = context_system::instance()->id;
    $token->creatorid = $wsuser->id;
    $token->timecreated = time();
    $token->validuntil = 0; // No expiry
    $DB->insert_record('external_tokens', $token);
    echo "✅ Token created: $tokenstring\n";
}
echo "\n";

// Step 8: Enable required capabilities for Manager role
echo "Step 8: Enabling required capabilities for Manager role...\n";
$capabilities = array(
    'webservice/rest:use',
    'moodle/webservice:createtoken',
    'moodle/course:view',
    'moodle/course:viewhiddencourses',
    'moodle/user:viewdetails',
    'moodle/user:viewhiddendetails',
    'moodle/grade:view',
    'moodle/grade:viewall',
    'moodle/site:accessallgroups',
);

$systemcontext = context_system::instance();
$enabled = 0;
$alreadyenabled = 0;

foreach ($capabilities as $capability) {
    // Check if capability exists
    if (!$DB->record_exists('capabilities', array('name' => $capability))) {
        echo "⚠️  Capability not found: $capability (skipping)\n";
        continue;
    }
    
    // Check current permission
    $currentperm = $DB->get_record('role_capabilities', array(
        'roleid' => $managerroleid,
        'capability' => $capability,
        'contextid' => $systemcontext->id
    ));
    
    if (!$currentperm || $currentperm->permission != CAP_ALLOW) {
        assign_capability($capability, CAP_ALLOW, $managerroleid, $systemcontext->id, true);
        echo "  ✅ Enabled: $capability\n";
        $enabled++;
    } else {
        $alreadyenabled++;
    }
}

echo "\n✅ Capabilities enabled: $enabled, already enabled: $alreadyenabled\n\n";

// Step 9: Clear caches
echo "Step 9: Clearing caches...\n";
purge_all_caches();
echo "✅ Caches cleared\n\n";

// Final summary
echo "=====================================\n";
echo "✅ SETUP COMPLETED SUCCESSFULLY!\n";
echo "=====================================\n\n";

echo "📋 SUMMARY:\n";
echo "  • Web services: ENABLED\n";
echo "  • REST protocol: ENABLED\n";
echo "  • Service: $servicename (ID: {$service->id})\n";
echo "  • Functions added: $added\n";
echo "  • Token: $tokenstring\n";
echo "  • User: {$wsuser->username} (ID: {$wsuser->id})\n";
echo "  • Role: Manager (assigned)\n";
echo "  • Capabilities: $enabled enabled\n\n";

echo "🧪 TEST TOKEN:\n";
echo "Run this in your browser:\n";
echo "http://localhost/moodle/webservice/rest/server.php?wstoken=$tokenstring&wsfunction=core_webservice_get_site_info&moodlewsrestformat=json\n\n";

echo "✅ You can now use the chatbot to query student grades!\n\n";

exit(0);
