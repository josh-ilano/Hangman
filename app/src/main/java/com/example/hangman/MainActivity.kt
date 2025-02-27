package com.example.hangman

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.example.hangman.ui.theme.HangmanTheme

val GoogleFont = FontFamily(
    Font(R.font.outfit_bold, FontWeight.Bold),
)

@ExperimentalLayoutApi
class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HangmanTheme {


                val array: Array<String> = resources.getStringArray(R.array.random_words)
                val configuration = LocalConfiguration.current

                val randomWord = rememberSaveable { array.random().uppercase() } // random word generated

                val id = resources.getIdentifier(randomWord.lowercase(), "string", packageName)
                val hint = rememberSaveable { getString(id) }

                var numHints by rememberSaveable { mutableStateOf(0) }

                var click by rememberSaveable { mutableStateOf(0) } // num of guesses
                var correctGuesses by rememberSaveable { mutableStateOf(List(randomWord.length) {false})  }
                // a boolean array. E.g. "CAT" would be {false, false, false}, and if user guesses a character
                // such as A, the array would become {false, true, false}

                var buttonStates by rememberSaveable {mutableStateOf(List(26) {true})}
                // boolena array which represents the buttons that cannot be repressed agaib

                if(click >= 6 || correctGuesses.all { it }) {
                    // if we exceeded clicks OR have won, then reset the game
                    Toast.makeText(LocalContext.current, if (click>=6) "LOSER" else "WINNER", Toast.LENGTH_SHORT).show()
                    finish()
                    startActivity(intent)
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        DoublePane(
                            modifier = Modifier.padding(innerPadding),
                            randomWord, click, correctGuesses, buttonStates,
                            hint, numHints
                        ) {newClick, newCorrectGuesses, newButtonStates, newNumHints ->
                            click = newClick
                            correctGuesses = newCorrectGuesses
                            buttonStates = newButtonStates
                            numHints = newNumHints}
                    }
                    else {
                        SinglePane(modifier = Modifier.padding(innerPadding),
                            randomWord, click, correctGuesses, buttonStates, hint,
                        numHints)
                        {newClick, newCorrectGuesses, newButtonStates, newNumHints ->
                            click = newClick
                            correctGuesses = newCorrectGuesses
                            buttonStates = newButtonStates
                            numHints = newNumHints
                        }
                    }

                }
            }
        }
    }
}


fun disableLetters(word: String, count: Int, boolList: List<Boolean>, keyboardList: List<Boolean>,
                   numHints: Int, updateInfo: (Int, List<Boolean>, List<Boolean>, Int) -> Unit) {

    var keyboardExcludeHalf = keyboardList.toMutableList()

    var numDisabled = 0; var bound: Int = 0
    for (enabled in keyboardList) {
        if (enabled) bound++
    }

    while (numDisabled < bound/2) {
        var letter = ('A'..'Z').random()

        if (letter !in word && keyboardExcludeHalf[letter.code-65]) {
            Log.d("LETTER", letter.toString())
            // if random letter is NOT in our word, disable keypress
            // IT ALSO must be enabled, so we can actually disable it
            keyboardExcludeHalf[letter.code-65] = false
            numDisabled++
        }

    }

    updateInfo(
        count+1,
        boolList,
        keyboardExcludeHalf,
        numHints
    )
}


fun revealVowels(word: String, count: Int, boolList: List<Boolean>, keyboardList: List<Boolean>,
                 numHints: Int, updateInfo: (Int, List<Boolean>, List<Boolean>, Int) -> Unit) {
    val vowels = arrayOf('A', 'E', 'I', 'O', 'U')

    var discoveredLetters = boolList.toMutableList()
    var keyboardExcludeVowels = keyboardList.toMutableList()

    for (letter in vowels) {
        if (letter in word) {
            word.mapIndexedNotNull { index, char ->
                if (char == letter) discoveredLetters[index] = true else null
            }
        }
        keyboardExcludeVowels[letter.code-65] = false


    }

    Log.d("Change", "Updating vowels")
    updateInfo(
        count+1, // counts as an automatic guess
        discoveredLetters.toList(),
        keyboardExcludeVowels.toList(),
        numHints)

}


/**
 * Represents the digital keyboard where we make our guesses
 */
@Composable
fun Keyboard(word: String, count: Int, boolList: List<Boolean>,
             keyboardList: List<Boolean>, numHints: Int,
             updateInfo: (Int, List<Boolean>, List<Boolean>, Int) -> Unit) {


    LazyVerticalGrid(columns=GridCells.Fixed(8), modifier=Modifier
        .background(
            Color(
                231,
                231,
                231,
                255
            )
        )
        .fillMaxWidth(.9f)
    ) {
        items(26) { index: Int ->


                val letter: Char = (index+65).toChar()

                TextButton(onClick = {
                    Log.d("LETTER", (index+65).toChar().toString())
                    Log.d("STRING", word)

                    /**
                     * 1) If we DON'T select a character in the word, we increase number of attempts
                     * 2) Reveal the correct words we selected by changing the boolean array that represents the word
                     * 3) Disable the button we just pressed
                     */
                    updateInfo(
                        if (letter in word) count else count+1, //
                        boolList.mapIndexed {index3, bool ->
                            if (word[index3] == letter) true else bool },
                        keyboardList.mapIndexed { index2, bool -> if (index2==index) false else bool },
                        numHints) },


                    colors = ButtonColors(
                        containerColor = Color(213, 213, 213, 255),
                        disabledContainerColor = Color(164, 164, 164, 255),
                        contentColor = Color(0,0,0,255),
                        disabledContentColor = Color(0,0,0,255)
                    ),
                    border = BorderStroke(2.dp, Color.Black),
                    enabled = keyboardList[index]
                ) { Text(letter.toString()) }


        }
    }



}


/**
 * Displays the word
 */
@ExperimentalLayoutApi
@Composable
fun GenerateWord(randomWord: String, boolList: List<Boolean>) {

     FlowRow(modifier = Modifier.fillMaxWidth(.9f),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center) {
            randomWord.forEachIndexed {
                index, letter ->
                Text(if (!boolList[index]) "  " else letter.toString(),
                    textDecoration = TextDecoration.Underline,
                    fontSize = 70.sp,
                    modifier = Modifier.background(Color(255,255,255,255)))
                Spacer(modifier = Modifier.width(15.dp))
            }
    }
}


fun DisplayImage(number: Int): Int {
    val x = when(number) {
        0 -> R.drawable.hangman_a
        1 -> R.drawable.hangman_b
        2 -> R.drawable.hangman_c
        3 -> R.drawable.hangman_d
        4 -> R.drawable.hangman_e
        5 -> R.drawable.hangman_f
        6 -> R.drawable.hangman_g
        else -> R.drawable.hangman_a
    }
    return x
}

@ExperimentalLayoutApi
@Composable
fun SinglePane(modifier: Modifier = Modifier, randomWord: String,
               click: Int, correctGuesses: List<Boolean>, buttonStates: List<Boolean>,
               hint: String, numHints: Int,
               updateInfo: (Int, List<Boolean>, List<Boolean>, Int) -> Unit) {

    LaunchedEffect(numHints) {
        Log.d("Num Hints", (numHints).toString())
        when(numHints) {
            2 -> { disableLetters(randomWord, click, correctGuesses, buttonStates, numHints, updateInfo) }
            3 -> { revealVowels(randomWord, click, correctGuesses, buttonStates, numHints, updateInfo) }
        }
    }

    Box(modifier=modifier
        .fillMaxSize()
        .background(Color(196, 196, 196, 255))) {
        Column(modifier=Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center)
        {
            TextButton(onClick = {

                updateInfo(click, correctGuesses, buttonStates, numHints+1)

            },
                enabled = if (numHints >= 3) false else true) { Text(if (numHints < 1) "Hint (${numHints})" else "Hint (${numHints}): $hint" )}
            Image(painter=painterResource(DisplayImage(click)), contentDescription = "Hangman",
                modifier = Modifier.fillMaxWidth(.7f))
            GenerateWord(randomWord, correctGuesses)
            Text(text="CHOOSE A LETTER",
                fontFamily = GoogleFont,
                fontWeight = FontWeight.Bold
            )
            Keyboard(randomWord, click, correctGuesses, buttonStates, numHints, updateInfo)

        }
    }
}


@ExperimentalLayoutApi
@Composable
fun DoublePane(modifier: Modifier = Modifier, randomWord: String,
               click: Int, correctGuesses: List<Boolean>, buttonStates: List<Boolean>,
               hint: String, numHints: Int,
               updateInfo: (Int, List<Boolean>, List<Boolean>, Int) -> Unit) {

    LaunchedEffect(numHints) {
        Log.d("Num Hints", (numHints).toString())
        when(numHints) {
            2 -> { disableLetters(randomWord, click, correctGuesses, buttonStates, numHints, updateInfo) }
            3 -> { revealVowels(randomWord, click, correctGuesses, buttonStates, numHints, updateInfo) }
        }
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier=Modifier.weight(0.35f)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(onClick = {

                    updateInfo(click, correctGuesses, buttonStates, numHints+1)

                },
                    enabled = if (numHints >= 3) false else true) { Text(
                    text=if (numHints < 1)
                        "Hint (${numHints})" else "Hint (${numHints}): $hint",
                        fontSize = 30.sp)}

                Text(text="CHOOSE A LETTER",
                    fontFamily = GoogleFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                )
                Keyboard(randomWord, click, correctGuesses, buttonStates, numHints, updateInfo)
            }
        }
        Box(modifier=Modifier.weight(0.65f)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painter=painterResource(DisplayImage(click)), contentDescription = "Hangman",
                    modifier = Modifier.fillMaxHeight(.7f))
                GenerateWord(randomWord, correctGuesses)
            }

        }

    }

}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HangmanTheme {
//        SinglePane(Modifier.fillMaxSize(), array)
    }
}