<?php
/**
 * Test if chatbot footer is rendering
 * Access: http://localhost/moodle/test_footer.php
 */

require_once(__DIR__ . '/config.php');
require_login();

$PAGE->set_context(context_system::instance());
$PAGE->set_url('/test_footer.php');
$PAGE->set_title('Test Footer Chatbot');
$PAGE->set_heading('Test Footer Chatbot');

echo $OUTPUT->header();

echo '<div class="container-fluid">';
echo '<h1>🔍 Test Footer Chatbot</h1>';
echo '<p>Scroll down to see if chatbot appears in footer</p>';
echo '<div style="padding: 50px; background: #f0f0f0; margin: 20px 0;">';
echo '<h3>Main Content Area</h3>';
echo '<p>Current theme: ' . $PAGE->theme->name . '</p>';
echo '<p>If chatbot is integrated, it should appear in the footer below.</p>';
echo '</div>';
echo '</div>';

echo $OUTPUT->footer();
