package com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio.user_allocation

//
//@Composable
//fun AllocationScreen(
//    viewModel: CreatePortfolioViewModel,
//    onBackClick: () -> Unit,
//    onSaveClick: () -> Unit
//) {
//    val selectedStocks by viewModel.selectedStocks.collectAsStateWithLifecycle()
//    val allocations by viewModel.allocations.collectAsStateWithLifecycle()
//    val totalAllocation by viewModel.totalAllocation.collectAsStateWithLifecycle()
//    val focusManager = LocalFocusManager.current
//
//    val isOverAllocated = totalAllocation > 100
//    val isPerfectlyAllocated = totalAllocation == 100
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(MaterialTheme.colorScheme.background)
//            .statusBarsPadding()
//            .imePadding()
//    ) {
//        // --- Header ---
//        Row(
//            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            IconButton(
//                onClick = onBackClick,
//                modifier = Modifier.clip(CircleShape)
//            ) {
//                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
//            }
//            Spacer(modifier = Modifier.width(12.dp))
//            Text(
//                text = "Set Weights",
//                style = MaterialTheme.typography.headlineSmall,
//                fontWeight = FontWeight.ExtraBold,
//                color = MaterialTheme.colorScheme.onBackground,
//                letterSpacing = (-0.5).sp,
//                modifier = Modifier.weight(1f)
//            )
//            Button(
//                onClick = {
//                    focusManager.clearFocus()
//                    onSaveClick()
//                },
//                enabled = isPerfectlyAllocated,
//                shape = RoundedCornerShape(12.dp),
//                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
//                elevation = ButtonDefaults.buttonElevation(0.dp)
//            ) {
//                Text("Save", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
//            }
//        }
//
//        // --- Allocation Progress Indicator ---
//        AllocationStatusBar(totalAllocation = totalAllocation)
//
//        // --- NEW: Contextual Info Banner ---
//        AllocationInfoBanner()
//
//        // --- List of Selected Stocks for Input ---
//        LazyColumn(
//            modifier = Modifier.weight(1f),
//            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            items(items = selectedStocks, key = { it.symbol }) { quote ->
//                val currentAlloc = allocations[quote.symbol] ?: 0
//                AllocationInputCard(
//                    quote = quote,
//                    currentAllocation = currentAlloc,
//                    isError = isOverAllocated,
//                    onAllocationChange = { newPercent ->
//                        viewModel.updateAllocation(quote.symbol, newPercent)
//                    }
//                )
//            }
//        }
//    }
//}
//
//// --- The New Info Banner Component ---
//@Composable
//fun AllocationInfoBanner() {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 24.dp, vertical = 8.dp)
//            .clip(RoundedCornerShape(12.dp))
//            // Using a very subtle secondary tint for a premium "informational" look
//            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
//            .padding(16.dp),
//        verticalAlignment = Alignment.Top
//    ) {
//        Icon(
//            imageVector = Icons.Rounded.Info,
//            contentDescription = "Information",
//            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
//            modifier = Modifier.size(20.dp).padding(top = 2.dp)
//        )
//        Spacer(modifier = Modifier.width(12.dp))
//        Text(
//            text = "Note: Distribute your weights to minimize risk. These assumed allocations allow us to calculate and generate a highly optimized portfolio result for your strategy.",
//            style = MaterialTheme.typography.bodySmall, // Inter font
//            color = MaterialTheme.colorScheme.onSurfaceVariant,
//            lineHeight = 18.sp
//        )
//    }
//}
//
//// (AllocationStatusBar and AllocationInputCard remain exactly the same as previously defined)
//
//// Visual Progress Bar to show 0 -> 100% Status
//@Composable
//fun AllocationStatusBar(totalAllocation: Int) {
//    // Animate progress width
//    val animatedProgress by animateFloatAsState(
//        targetValue = (totalAllocation / 100f).coerceIn(0f, 1f),
//        animationSpec = tween(500),
//        label = "ProgressBarAnimation"
//    )
//
//    // Animate color based on state (Green if 100%, Red if > 100%, Primary otherwise)
//    val progressColor by animateColorAsState(
//        targetValue = when {
//            totalAllocation > 100 -> MaterialTheme.colorScheme.error
//            totalAllocation == 100 -> Color(0xFF388E3C) // A solid success green
//            else -> MaterialTheme.colorScheme.primary
//        }, label = "ProgressColorAnimation"
//    )
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 24.dp, vertical = 8.dp)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 8.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.Bottom
//        ) {
//            Text(
//                text = "Total Allocated",
//                style = MaterialTheme.typography.labelLarge,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//            Text(
//                text = "$totalAllocation%",
//                style = MaterialTheme.typography.titleLarge,
//                fontWeight = FontWeight.ExtraBold,
//                color = progressColor
//            )
//        }
//
//        // Custom minimalist progress bar
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(8.dp)
//                .clip(CircleShape)
//                .background(MaterialTheme.colorScheme.surfaceVariant)
//        ) {
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth(animatedProgress)
//                    .fillMaxHeight()
//                    .clip(CircleShape)
//                    .background(progressColor)
//            )
//        }
//
//        if (totalAllocation > 100) {
//            Text(
//                text = "Exceeds 100%. Please reduce weights.",
//                style = MaterialTheme.typography.labelSmall,
//                color = MaterialTheme.colorScheme.error,
//                modifier = Modifier.padding(top = 4.dp)
//            )
//        }
//    }
//}
//
//// Flat Card with Numeric Input
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AllocationInputCard(
//    quote: Quote, currentAllocation: Int, isError: Boolean, onAllocationChange: (Int) -> Unit
//) {
//    // Local state to handle temporary empty string input (e.g., user clears field to type a new number)
//    var textValue by remember(currentAllocation) {
//        mutableStateOf(if (currentAllocation == 0) "" else currentAllocation.toString())
//    }
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
//        border = androidx.compose.foundation.BorderStroke(
//            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
//        )
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            // Left: Stock Info
//            Column(
//                modifier = Modifier
//                    .weight(1f)
//                    .padding(end = 16.dp)
//            ) {
//                Text(
//                    text = quote.symbol,
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.ExtraBold,
//                    color = MaterialTheme.colorScheme.onSurface
//                )
//                Spacer(modifier = Modifier.height(4.dp))
//                Text(
//                    text = quote.longName ?: quote.shortName ?: "",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//            }
//
//            // Right: Percent Input
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                OutlinedTextField(
//                    value = textValue,
//                    onValueChange = { newValue ->
//                        // Only allow digits, max 3 characters (e.g., 100)
//                        val cleanString = newValue.filter { it.isDigit() }.take(3)
//                        textValue = cleanString
//
//                        val newInt = cleanString.toIntOrNull() ?: 0
//                        onAllocationChange(newInt)
//                    },
//                    modifier = Modifier.width(80.dp),
//                    textStyle = LocalTextStyle.current.copy(
//                        textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp
//                    ),
//                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), // Forces number pad
//                    singleLine = true,
//                    isError = isError,
//                    shape = RoundedCornerShape(12.dp),
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedContainerColor = MaterialTheme.colorScheme.background,
//                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
//                        focusedBorderColor = MaterialTheme.colorScheme.primary,
//                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
//                    )
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text(
//                    text = "%",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//        }
//    }
//}