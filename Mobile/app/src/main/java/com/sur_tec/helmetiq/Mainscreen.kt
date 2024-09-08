package com.sur_tec.helmetiq

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sur_tec.helmetiq.ui.theme.customColors

@Composable

fun Mainscreen(navController: NavHostController, modifier: Modifier = Modifier) {

    var switchState by rememberSaveable {
        mutableStateOf(false)
    }
    Column(
        modifier = modifier
            .fillMaxSize(),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        HeaderTitle()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Helmet Image
            HelmetImage()

            // Battery and Bluetooth Icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically

            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_battery),
                    contentDescription = "Battery",
                    tint = MaterialTheme.colorScheme.primary,  // Use a bold primary color
                    modifier = Modifier.size(40.dp)  // Larger icon size
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_bluetooth),
                    contentDescription = "Bluetooth",
                    tint = MaterialTheme.colorScheme.secondary,  // Use a contrasting color
                    modifier = Modifier.size(40.dp)  // Larger icon size
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Headlights Toggle
            HeadLight(switchState) {
                switchState = it
            }
        }

        // Placeholder for Map, replace with an actual map implementation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = "Map View Placeholder",
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        customColors.surface
                    )
            ) {
                Text(
                    text = "Total Distance Traveled: 10 Miles",
                    color = customColors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        customColors.surface
                    )
            ) {
                Text(
                    text = "Ride Time: 58 Minutes",
                    color = customColors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(16.dp)
                )
            }

        }

    }
}

@Composable
private fun HeadLight(switchState: Boolean = false, onSwitchChanged: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(customColors.surface)
            .shadow(elevation = 4.dp)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Headlights",
            fontSize = 18.sp,
            color = customColors.onSurface,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = switchState,
            onCheckedChange = onSwitchChanged,
            colors = SwitchDefaults.colors(
                checkedThumbColor = customColors.secondary,
                uncheckedThumbColor = customColors.inversePrimary
            )
        )
    }
}


@Composable
@Preview
private fun HelmetImage() {
    Image(
        painter = painterResource(id = R.drawable.helmet_man_removebg), // Replace with your image resource
        contentDescription = "Helmet",
        modifier = Modifier.size(170.dp),
        contentScale = ContentScale.Crop
    )
}

@Composable
@Preview(showBackground = true)
private fun HeaderTitle() {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "HelmetIQ",
            fontSize = 30.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Black, // Use heavy, bold font for modern design
            fontFamily = FontFamily.SansSerif, // Switch to modern sans-serif fonts
            color = customColors.primary,
            modifier = Modifier.padding(top = 16.dp, start = 12.dp)

        )
    }
}