package com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio.investment_strategy

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio.CreatePortfolioViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio.RiskProfile
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio.TimeHorizonUnit
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentStrategyScreen(
    viewModel: CreatePortfolioViewModel,
    onBackClick: () -> Unit,
    onGenerateClick: () -> Unit
) {
    val capital by viewModel.investmentCapital.collectAsStateWithLifecycle()
    val risk by viewModel.riskProfile.collectAsStateWithLifecycle()
    val time by viewModel.timeHorizon.collectAsStateWithLifecycle()
    val timeUnit by viewModel.timeHorizonUnit.collectAsStateWithLifecycle()
    val isValid by viewModel.isStrategyValid.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        // --- Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant) // Pure M3
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Strategy",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onGenerateClick()
                },
                enabled = isValid,
                shape = CircleShape, // Pill shape is more modern M3
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text("Generate", fontWeight = FontWeight.Bold)
            }
        }

        // --- Form Content (Using verticalScroll instead of LazyColumn) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // 1. Initial Investment
            Column {
                Text(
                    text = "Initial Capital",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = capital,
                    onValueChange = viewModel::updateCapital,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., 100000", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Rounded.CurrencyRupee, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    // UX FIX: Changed from NumberPassword to Number, added Next action
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                )
            }

            // 2. Risk Temperament
            Column {
                Text(
                    text = "Risk Temperament",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RiskProfile.entries.forEach { profile ->
                        RiskProfileCard(
                            profile = profile,
                            isSelected = risk == profile,
                            onClick = {
                                focusManager.clearFocus() // Hide keyboard if they click a card
                                viewModel.updateRiskProfile(profile)
                            }
                        )
                    }
                }
            }

            // 3. Time Horizon
            Column {
                Text(
                    text = "Time Horizon",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = time,
                        onValueChange = viewModel::updateTimeHorizon,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Duration", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = { Icon(Icons.Rounded.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        // UX FIX: Added Done action to close keyboard
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    )

                    // Custom Segmented Toggle for Years/Months using Pure M3
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant, // Gray track background
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(modifier = Modifier.height(56.dp).padding(4.dp)) {
                            TimeUnitToggle(
                                text = "Years",
                                isSelected = timeUnit == TimeHorizonUnit.YEARS,
                                onClick = { viewModel.updateTimeHorizonUnit(TimeHorizonUnit.YEARS) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            TimeUnitToggle(
                                text = "Months",
                                isSelected = timeUnit == TimeHorizonUnit.MONTHS,
                                onClick = { viewModel.updateTimeHorizonUnit(TimeHorizonUnit.MONTHS) }
                            )
                        }
                    }
                }
            }

            // Add some padding at the bottom so the last item isn't hugging the screen edge
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ==========================================
// REFINED UI COMPONENTS
// ==========================================

@Composable
fun RiskProfileCard(
    profile: RiskProfile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Pure M3 Semantic Colors
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "BorderColor"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        label = "ContainerColor"
    )
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) contentColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun TimeUnitToggle(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        label = "ToggleBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "ToggleText"
    )

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}