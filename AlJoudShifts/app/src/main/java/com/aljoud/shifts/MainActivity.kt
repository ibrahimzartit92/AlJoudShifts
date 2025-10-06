package com.aljoud.shifts

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aljoud.shifts.ui.screens.BranchShiftsScreen
import com.aljoud.shifts.ui.screens.BranchesScreen
import com.aljoud.shifts.ui.screens.EmployeeDetailScreen
import com.aljoud.shifts.ui.screens.EmployeesScreen
import com.aljoud.shifts.ui.screens.HomeScreen
import com.aljoud.shifts.ui.screens.SettingsScreen
import com.aljoud.shifts.ui.screens.ShiftsScreen
import com.aljoud.shifts.ui.screens.ShiftTemplatesScreen   // ← أضفنا الاستيراد
import com.aljoud.shifts.ui.theme.AlJoudShiftsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlJoudShiftsTheme {
                AppNav()
            }
        }
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "home") {

        composable("home") {
            HomeScreen(onNavigate = { route -> nav.navigate(route) })
        }

        // شاشة قائمة الموظفين
        composable("employees") {
            EmployeesScreen(
                onEmployeeClick = { id, name ->
                    val encodedName = Uri.encode(name)
                    nav.navigate("employee/$id/$encodedName")
                }
            )
        }

        // شاشة قوالب الشفتات (مسار مستقل، مو داخل employees)
        composable("templates") {
            ShiftTemplatesScreen()
        }

        // تفاصيل موظف
        composable(
            route = "employee/{id}/{name}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            val name = backStackEntry.arguments?.getString("name") ?: "موظف"
            EmployeeDetailScreen(employeeId = id, employeeName = name)
        }

        // شاشة الشفتات: تعرض قائمة الفروع وتنتقل لشفتات الفرع
        composable("shifts") {
            ShiftsScreen(
                onOpenBranch = { id, name ->
                    val encoded = Uri.encode(name)
                    nav.navigate("branch_shifts/$id/$encoded")
                }
            )
        }

        // شاشة شفتات فرع معيّن
        composable(
            route = "branch_shifts/{branchId}/{branchName}",
            arguments = listOf(
                navArgument("branchId") { type = NavType.LongType },
                navArgument("branchName") { type = NavType.StringType }
            )
        ) { backStack ->
            val bid = backStack.arguments?.getLong("branchId") ?: return@composable
            val name = backStack.arguments?.getString("branchName") ?: "فرع"
            BranchShiftsScreen(branchId = bid, branchName = name)
        }

        // باقي الشاشات
        composable("branches") { BranchesScreen() }
        composable("settings") { SettingsScreen() }
    }
}
