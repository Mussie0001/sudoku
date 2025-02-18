package com.example.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.sudoku.ui.theme.SudokuTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SudokuTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    SudokuGame()
                    }
                }
            }
        }
    }

@Composable
fun SudokuGame() {
    // snackbar for win/error messages
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 9x9 mutable state list of state lists for board
    // null = empty cell
    val board: SnapshotStateList<SnapshotStateList<Int?>> = remember {
        mutableStateListOf<SnapshotStateList<Int?>>().apply {
            // randomly filling first row with numbers 1-9
            add((1..9).shuffled().toList().toMutableStateList())
            // all cells empty (null) for the next 8 rows
            repeat(8) { add(List(9) { null }.toMutableStateList()) }
        }
    }
    val lockedCells: SnapshotStateList<SnapshotStateList<Boolean>> = remember {
        mutableStateListOf<SnapshotStateList<Boolean>>().apply {
            // first row cannot be edited
            add(List(9) { true }.toMutableStateList())
            // any user entered number can be edited after entering a value
            repeat(8) { add(List(9) { false }.toMutableStateList()) }
        }
    }

    // state for the selected cell [row, col] for input
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    // input dialog state
    var showDialog by remember { mutableStateOf(false) }
    // user input text
    var inputText by remember { mutableStateOf("") }

    // board reset function
    fun resetBoard() {
        board.clear()
        board.add((1..9).shuffled().toList().toMutableStateList())
        repeat(8) { board.add(List(9) { null }.toMutableStateList()) }
    }

    // validates [row,col] placement of given value
    fun validMove(row: Int, col: Int, value: Int): Boolean {

        for (c in 0 until 9) {
            if (board[row][c] == value) return false
        }
        for (r in 0 until 9) {
            if (board[r][col] == value) return false
        }

        val startRow = (row / 3) * 3
        val startCol = (col / 3) * 3
        for (r in startRow until startRow + 3) {
            for (c in startCol until startCol + 3) {
                if (board[r][c] == value) return false
            }
        }
        return true
    }

    // determines if a user won -- board is filled and valid
    fun isWin(): Boolean {
        // every cell has a value
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                if (board[r][c] == null) return false
            }
        }
        // check for duplicates in row
        for (r in 0 until 9) {
            val seen = mutableSetOf<Int>()
            for (c in 0 until 9) {
                val v = board[r][c]!!
                if (v in seen) return false
                seen.add(v)
            }
        }
        // check for duplicates in column
        for (c in 0 until 9) {
            val seen = mutableSetOf<Int>()
            for (r in 0 until 9) {
                val v = board[r][c]!!
                if (v in seen) return false
                seen.add(v)
            }
        }
        // check for duplicates in 3x3 blocks
        for (blockRow in 0 until 3) {
            for (blockCol in 0 until 3) {
                val seen = mutableSetOf<Int>()
                for (r in blockRow * 3 until blockRow * 3 + 3) {
                    for (c in blockCol * 3 until blockCol * 3 + 3) {
                        val v = board[r][c]!!
                        if (v in seen) return false
                        seen.add(v)
                    }
                }
            }
        }
        return true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { resetBoard() },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text("Reset")
            }
            // grid using LazyVerticalGrid with 9 columns
            val gridState = rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Fixed(9),
                state = gridState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(81) { index ->
                    //mapping the flat list index into 2D grid coordinates
                    val row = index / 9
                    val col = index % 9
                    // number from the board
                    val cellValue = board[row][col]
                    val isLocked = lockedCells[row][col]
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(
                                // background for visual each 3x3 block
                                if (((row / 3) + (col / 3)) % 2 == 0) Color(0xFFD0E8F2) else Color(0xFFA0A0A0)
                            )
                            // allow user to re-enter a value if not in the first row
                            .clickable(enabled = !isLocked) {
                                selectedCell = row to col
                                inputText = cellValue?.toString() ?: ""
                                showDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cellValue?.toString() ?: "",
                            fontSize = 20.sp,
                            color = if (cellValue != null) Color.Black else Color.Gray
                        )
                    }
                }
            }
        }
    }

    // input dialogue for entering a number
    if (showDialog && selectedCell != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Enter a number (1-9)") },
            text = {
                TextField(
                    value = inputText,
                    onValueChange = { newValue ->
                        // input validation
                        if (newValue.length <= 1 && (newValue.isEmpty() || newValue.matches(Regex("[1-9]")))) {
                            inputText = newValue
                        }
                    },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val number = inputText.toIntOrNull()
                        if (number != null && selectedCell != null) {
                            val (row, col) = selectedCell!!
                            // move validation
                            if (validMove(row, col, number)) {
                                board[row][col] = number
                                showDialog = false
                                selectedCell = null
                                // check for win after each valid turn
                                if (isWin()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("You won!")
                                    }
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Invalid move")
                                }
                            }
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDialog = false
                        selectedCell = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SudokuTheme {
        SudokuGame()
    }
}