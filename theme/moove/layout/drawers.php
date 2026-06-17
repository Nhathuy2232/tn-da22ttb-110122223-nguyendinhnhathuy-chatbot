<?php
// This file is part of Moodle - http://moodle.org/
//
// Moodle is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// Moodle is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Moodle.  If not, see <http://www.gnu.org/licenses/>.

/**
 * A drawer based layout for the moove theme.
 *
 * @package    theme_moove
 * @copyright  2022 Willian Mano {@link https://conecti.me}
 * @license    http://www.gnu.org/copyleft/gpl.html GNU GPL v3 or later
 */

defined('MOODLE_INTERNAL') || die();

require_once($CFG->libdir . '/behat/lib.php');
require_once($CFG->dirroot . '/course/lib.php');

function theme_moove_chatbot_resolve_role($userid): string {
    global $DB, $CFG;

    // 1. Site admin luôn là ADMIN
    if (is_siteadmin($userid)) {
        return 'ADMIN';
    }

    $systemcontext = \context_system::instance();

    // Lấy tất cả shortname role của user ở system context
    $userroles = $DB->get_records_sql(
        "SELECT r.shortname
           FROM {role_assignments} ra
           JOIN {role} r ON r.id = ra.roleid
           JOIN {context} c ON c.id = ra.contextid
          WHERE ra.userid = :userid
            AND c.contextlevel = :syslevel",
        ['userid' => $userid, 'syslevel' => CONTEXT_SYSTEM]
    );

    $roleshorts = [];
    foreach ($userroles as $r) {
        $roleshorts[] = $r->shortname;
    }

    // Nếu không có role ở system context, kiểm tra ở mọi context
    if (empty($roleshorts)) {
        $allroles = $DB->get_records_sql(
            "SELECT r.shortname
               FROM {role_assignments} ra
               JOIN {role} r ON r.id = ra.roleid
              WHERE ra.userid = :userid",
            ['userid' => $userid]
        );
        foreach ($allroles as $r) {
            $roleshorts[] = $r->shortname;
        }
    }

    if (empty($roleshorts)) {
        return 'STUDENT';
    }

    // Thứ tự ưu tiên: ADMIN > ADVISER > LECTURER > STUDENT
    // Một user có thể có nhiều role, role cao nhất sẽ thắng
    $priority = [
        'ADMIN'    => ['manager', 'admin'],
        'ADVISER'  => ['academicadviser', 'adviser', 'covan', 'coursecreator'],
        'LECTURER' => ['editingteacher', 'teacher'],
        'STUDENT'  => ['student'],
    ];

    foreach ($priority as $resolved => $shortnames) {
        foreach ($shortnames as $sn) {
            if (in_array($sn, $roleshorts, true)) {
                return $resolved;
            }
        }
    }

    return 'STUDENT';
}

// Add block button in editing mode.
$addblockbutton = $OUTPUT->addblockbutton();

if (isloggedin()) {
    $courseindexopen = (get_user_preferences('drawer-open-index', true) == true);
    $blockdraweropen = (get_user_preferences('drawer-open-block') == true);
} else {
    $courseindexopen = false;
    $blockdraweropen = false;
}

if (defined('BEHAT_SITE_RUNNING') && get_user_preferences('behat_keep_drawer_closed') != 1) {
    $blockdraweropen = true;
}

$extraclasses = ['uses-drawers'];
if ($courseindexopen) {
    $extraclasses[] = 'drawer-open-index';
}

$blockshtml = $OUTPUT->blocks('side-pre');
$hasblocks = (strpos($blockshtml, 'data-block=') !== false || !empty($addblockbutton));
if (!$hasblocks) {
    $blockdraweropen = false;
}

$themesettings = new \theme_moove\util\settings();

if (!$themesettings->enablecourseindex) {
    $courseindex = '';
} else {
    $courseindex = core_course_drawer();
}

if (!$courseindex) {
    $courseindexopen = false;
}

$forceblockdraweropen = $OUTPUT->firstview_fakeblocks();

$secondarynavigation = false;
$overflow = '';
if ($PAGE->has_secondary_navigation()) {
    $secondary = $PAGE->secondarynav;

    if ($secondary->get_children_key_list()) {
        $tablistnav = $PAGE->has_tablist_secondary_navigation();
        $moremenu = new \core\navigation\output\more_menu($PAGE->secondarynav, 'nav-tabs', true, $tablistnav);
        $secondarynavigation = $moremenu->export_for_template($OUTPUT);
        $extraclasses[] = 'has-secondarynavigation';
    }

    $overflowdata = $PAGE->secondarynav->get_overflow_menu_data();
    if (!is_null($overflowdata)) {
        $overflow = $overflowdata->export_for_template($OUTPUT);
    }
}

$primary = new core\navigation\output\primary($PAGE);
$renderer = $PAGE->get_renderer('core');
$primarymenu = $primary->export_for_template($renderer);
$buildregionmainsettings = !$PAGE->include_region_main_settings_in_header_actions() && !$PAGE->has_secondary_navigation();
// If the settings menu will be included in the header then don't add it here.
$regionmainsettingsmenu = $buildregionmainsettings ? $OUTPUT->region_main_settings_menu() : false;

$header = $PAGE->activityheader;
$headercontent = $header->export_for_template($renderer);

$bodyattributes = $OUTPUT->body_attributes($extraclasses);
$templatecontext = [
    'sitename' => format_string($SITE->shortname, true, ['context' => \core\context\course::instance(SITEID), "escape" => false]),
    'output' => $OUTPUT,
    'sidepreblocks' => $blockshtml,
    'hasblocks' => $hasblocks,
    'bodyattributes' => $bodyattributes,
    'courseindexopen' => $courseindexopen,
    'blockdraweropen' => $blockdraweropen,
    'courseindex' => $courseindex,
    'primarymoremenu' => $primarymenu['moremenu'],
    'secondarymoremenu' => $secondarynavigation ?: false,
    'mobileprimarynav' => $primarymenu['mobileprimarynav'],
    'usermenu' => $primarymenu['user'],
    'langmenu' => $primarymenu['lang'],
    'forceblockdraweropen' => $forceblockdraweropen,
    'regionmainsettingsmenu' => $regionmainsettingsmenu,
    'hasregionmainsettingsmenu' => !empty($regionmainsettingsmenu),
    'overflow' => $overflow,
    'headercontent' => $headercontent,
    'addblockbutton' => $addblockbutton,
    'enablecourseindex' => $themesettings->enablecourseindex,
    // CRITICAL: Add username for chatbot role detection
    'currentusername' => $USER->username,
    'currentuserid' => $USER->id,
    'currentuserrole' => theme_moove_chatbot_resolve_role($USER->id),
    'currentusersession' => session_id(),
];

$templatecontext = array_merge($templatecontext, $themesettings->footer());

// Add chatbot data for footer
$templatecontext['chatbot_api'] = 'http://localhost:8082/api/chat/message';
$templatecontext['chatbot_username'] = $USER->username;
$templatecontext['chatbot_userid'] = $USER->id;
$templatecontext['chatbot_sessionid'] = session_id();
$templatecontext['chatbot_role'] = theme_moove_chatbot_resolve_role($USER->id);

echo $OUTPUT->render_from_template('theme_moove/drawers', $templatecontext);
