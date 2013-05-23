<?php
$secret = 'secret1'; // To make the hash more difficult to reproduce.
$path   = '/vod/index.html'; // This is the file to send to the user.
$expire = time() + 30;; // At which point in time the file should expire. time() + x; would be the usual usage.
echo $expire."\n";
$md5 = base64_encode(md5($secret . $path . $expire, true)); // Using binary hashing.
$md5 = strtr($md5, '+/', '-_'); // + and / are considered special characters in URLs, see the wikipedia page linked in references.
$md5 = str_replace('=', '', $md5); // When used in query parameters the base64 padding character is considered special.

echo $path."?st=".$md5."&e=".$expire."\n";
?>

