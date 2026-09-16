#!/usr/bin/env python3
"""Present the existing, working in-game menu as a centered Pocket GBA panel."""
from pathlib import Path
import sys

root = Path(sys.argv[1])
base = root / "lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/gamemenu"
activity = base / "GameMenuActivity.kt"
home = base / "GameMenuHomeScreen.kt"

def one(source: str, old: str, new: str, label: str) -> str:
    if source.count(old) != 1:
        raise SystemExit(f'Pocket game menu integration changed upstream: {label}')
    return source.replace(old, new, 1)

text = activity.read_text()
text = one(text, 'contentAlignment = Alignment.CenterEnd,', 'contentAlignment = Alignment.Center,', 'center panel')
text = one(text, 'minOf(maxWidth * 0.8f, 400f.dp)', 'minOf(maxWidth - 32.dp, 420.dp)', 'panel width')
text = one(text, '''                        .padding()
                        .fillMaxHeight()
                        .width(panelWidth)''', '''                        .padding(vertical = 18.dp)
                        .fillMaxHeight(0.82f)
                        .width(panelWidth)''', 'panel height')
activity.write_text(text)

text = home.read_text()
text = one(text, 'import androidx.compose.foundation.layout.Column', '''import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape''', 'layout imports')
text = one(text, 'import androidx.compose.material3.Icon', '''import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme''', 'material imports')
text = one(text, 'import androidx.compose.ui.Modifier', '''import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp''', 'style imports')
text = one(text,
    '    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {',
    '''    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(
            text = gameMenuRequest.game.title,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
        )
        Button(
            onClick = { onResult { } },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Continuar")
        }
        Spacer(modifier = Modifier.height(12.dp))''',
    'menu hero and continue')
home.write_text(text)
print('Pocket GBA in-game menu: centered card and continue button ready')
