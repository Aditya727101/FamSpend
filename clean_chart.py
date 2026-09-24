with open('app/src/main/java/com/example/ui/components/SpendingTrendChart.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for line in lines:
    if '",\n' in line and 'x.coerceIn(20f' in line:
        continue # this is part of the garbage left by regex
    if 'x.coerceIn(20f' in line:
        continue
    if 'height - 5.dp.toPx(),' in line and skip:
        continue
    if 'labelPaint' in line and skip:
        skip = False
        continue
    if ')' in line and skip:
        skip = False
        continue
    if '} ' in line and skip:
        skip = False
        continue
    new_lines.append(line)

with open('app/src/main/java/com/example/ui/components/SpendingTrendChart.kt', 'w') as f:
    f.writelines(new_lines)

