package com.example.hangman


/**
 * Assisted by Chat-GPT. The issue was I was creating too many saveable states and passing all of them
 * to the various function throughout MainActivity, therefore I looked for an alternative where I
 * could centralize the game data. This allows me to avoid:
 *
 *  var randomWord by rememberSaveable { mutableStateOf(array.random()) } // random wod generated
 *  var click by rememberSaveable { mutableStateOf(1) } // num of guesses
 *  var correctGuesses by rememberSaveable { mutableStateOf(List(randomWord.length) {false})  }
 *
 */
//@Parcelize
//data class GameState(
//    val randomWord: String,
//    val click: Int,
//    val correctGuesses: List<Boolean>,
//    val buttonStates: List<Boolean> = List(26) { true } // Track keyboard state too!
//)
