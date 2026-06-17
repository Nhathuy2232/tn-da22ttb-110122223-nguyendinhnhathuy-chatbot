<?php
/**
 * Emergency cache clearer for Moodle
 * Access via: http://localhost/moodle/clear_cache.php
 */

define('CLI_SCRIPT', false);
define('CACHE_DISABLE_ALL', true);

require_once(__DIR__ . '/config.php');
require_once($CFG->libdir . '/adminlib.php');

require_login();
require_capability('moodle/site:config', context_system::instance());

// Purge all caches
purge_all_caches();

// Clear mustache/template cache specifically
$mustachedir = $CFG->localcachedir . '/mustache';
if (is_dir($mustachedir)) {
    remove_dir($mustachedir, true);
    echo "✅ Mustache cache cleared<br>";
}

// Clear theme cache
$themecache = $CFG->dataroot . '/cache/theme';
if (is_dir($themecache)) {
    remove_dir($themecache, true);
    echo "✅ Theme cache cleared<br>";
}

// Clear local cache
$localcache = $CFG->localcachedir;
if (is_dir($localcache)) {
    $files = glob($localcache . '/*');
    foreach ($files as $file) {
        if (is_dir($file)) {
            remove_dir($file, true);
        } else {
            @unlink($file);
        }
    }
    echo "✅ Local cache cleared<br>";
}

echo "<h2>✅ All caches purged successfully!</h2>";
echo "<p><a href='{$CFG->wwwroot}'>Back to home</a></p>";
echo "<p><strong>Now press Ctrl+Shift+R to hard refresh your browser!</strong></p>";
