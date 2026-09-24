import re

with open('gradle/libs.versions.toml', 'r') as f:
    libs = f.read()

if 'ui-text-google-fonts' not in libs:
    libs = libs.replace('androidx-compose-ui = {', 'androidx-compose-ui-text-google-fonts = { group = "androidx.compose.ui", name = "ui-text-google-fonts" }\nandroidx-compose-ui = {')

with open('gradle/libs.versions.toml', 'w') as f:
    f.write(libs)

with open('app/build.gradle.kts', 'r') as f:
    build = f.read()

if 'ui.text.google.fonts' not in build:
    build = build.replace('implementation(libs.androidx.compose.ui)', 'implementation(libs.androidx.compose.ui)\n    implementation(libs.androidx.compose.ui.text.google.fonts)')

with open('app/build.gradle.kts', 'w') as f:
    f.write(build)
