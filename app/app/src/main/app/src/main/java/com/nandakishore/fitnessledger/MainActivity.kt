package com.nandakishore.fitnessledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.aggregate.AggregateRequest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectClient: HealthConnectClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        healthConnectClient =
            HealthConnectClient.getOrCreate(this)

        setContent {
            FitnessLedgerApp()
        }
    }

    @Composable
    fun FitnessLedgerApp() {

        var steps by remember { mutableLongStateOf(0L) }
        var permissionGranted by remember {
            mutableStateOf(false)
        }

        val scope = rememberCoroutineScope()

        val permissions = remember {
            setOf(
                HealthPermission.getReadPermission(
                    StepsRecord::class
                )
            )
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement =
                    Arrangement.spacedBy(16.dp)
            ) {

                item {

                    Text(
                        text = "Fitness Ledger",
                        style = MaterialTheme.typography.headlineLarge
                    )

                    Text(
                        text = "Your fitness. Your progress. Your ledger.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                item {

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {

                            Text(
                                text = "Today's Steps",
                                style =
                                    MaterialTheme.typography.titleLarge
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Text(
                                text = steps.toString(),
                                style =
                                    MaterialTheme.typography.displayMedium
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            Button(
                                onClick = {

                                    scope.launch {

                                        val granted =
                                            healthConnectClient
                                                .permissionController
                                                .getGrantedPermissions()

                                        permissionGranted =
                                            granted.containsAll(
                                                permissions
                                            )

                                        if (permissionGranted) {

                                            steps =
                                                readTodaySteps()

                                        }
                                    }
                                }
                            ) {

                                Text("Refresh Steps")
                            }
                        }
                    }
                }

                item {

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {

                            Text(
                                text = "Today's Goals",
                                style =
                                    MaterialTheme.typography.titleLarge
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            GoalRow(
                                "Calories",
                                "Set later"
                            )

                            GoalRow(
                                "Protein",
                                "Set later"
                            )

                            GoalRow(
                                "Water",
                                "Set later"
                            )

                            GoalRow(
                                "Steps",
                                "10,000"
                            )
                        }
                    }
                }

                item {

                    Text(
                        text = "Coming next",
                        style =
                            MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text =
                            "Food • Running • Weight • Progress • Coach • Strava"
                    )
                }
            }
        }
    }

    @Composable
    fun GoalRow(
        name: String,
        value: String
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(name)
            Text(value)
        }
    }

    private suspend fun readTodaySteps(): Long {

        val zone =
            ZoneId.systemDefault()

        val start =
            java.time.LocalDate
                .now(zone)
                .atStartOfDay(zone)
                .toInstant()

        val end =
            Instant.now()

        val request =
            AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL
                ),
                timeRangeFilter =
                    TimeRangeFilter.between(
                        start,
                        end
                    )
            )

        val result =
            healthConnectClient.aggregate(request)

        return result[
            StepsRecord.COUNT_TOTAL
        ] ?: 0L
    }
}
