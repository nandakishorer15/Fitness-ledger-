package com.nandakishore.fitnessledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    private lateinit var healthConnectClient: HealthConnectClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        healthConnectClient = HealthConnectClient.getOrCreate(this)
        setContent { FitnessLedgerApp() }
    }

    @Composable
    private fun FitnessLedgerApp() {
        val scope = rememberCoroutineScope()
        var steps by remember { mutableLongStateOf(0L) }
        var status by remember { mutableStateOf("Connect Health Connect to read your steps.") }
        val permissions = remember {
            setOf(HealthPermission.getReadPermission(StepsRecord::class))
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { granted ->
            if (granted.containsAll(permissions)) {
                status = "Health Connect connected."
                scope.launch { steps = readTodaySteps() }
            } else {
                status = "Step permission was not granted."
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Fitness Ledger", style = MaterialTheme.typography.headlineLarge)
                    Text("Your fitness. Your progress. Your ledger.")
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Today's Steps", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Text("$steps", style = MaterialTheme.typography.displayMedium)
                            Text("Goal: 10,000 steps")
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { permissionLauncher.launch(permissions) }) {
                                    Text("Connect Steps")
                                }
                                Button(onClick = { scope.launch { steps = readTodaySteps() } }) {
                                    Text("Refresh")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(status)
                        }
                    }
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Today's Goals", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(10.dp))
                            GoalRow("Calories", "Coming next")
                            GoalRow("Protein", "Coming next")
                            GoalRow("Water", "Coming next")
                            GoalRow("Steps", "10,000")
                        }
                    }
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Fitness Ledger", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Text("Food • Running • Weight • Progress • Coach • Strava")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun GoalRow(name: String, value: String) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name)
            Text(value)
        }
    }

    private suspend fun readTodaySteps(): Long {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val result = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, Instant.now())
            )
        )
        return result[StepsRecord.COUNT_TOTAL] ?: 0L
    }
}
