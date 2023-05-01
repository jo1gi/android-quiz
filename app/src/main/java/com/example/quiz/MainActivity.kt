package com.example.quiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quiz.ui.theme.QuizTheme

enum class State {
    SelectQuiz,
    AnswerQuestions,
}

data class Question(
    val question: String,
    val correctAnswer: String,
    val incorrectAnswers: List<String>,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val (state, changeState) = rememberSaveable { mutableStateOf(State.AnswerQuestions) }
            QuizTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(all = 16.dp),
                    color = MaterialTheme.colors.background
                ) {
                    if(state == State.SelectQuiz) {
                        SelectQuiz{ _, _ ->
                            changeState(State.AnswerQuestions)
                        }
                    } else if(state == State.AnswerQuestions) {
                        val questions = listOf(
                            Question("Where is A?", "A", listOf("B", "C", "D")),
                            Question("Where is B?", "B", listOf("A", "C", "D")),
                            Question("Where is C?", "C", listOf("A", "B", "D")),
                        )
                        AnswerQuestions(questions){ changeState(State.SelectQuiz) }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectQuiz(startQuiz: (String, String) -> Unit) {
    val (difficulty, updateDifficulty) = rememberSaveable { mutableStateOf("Medium") }
    val (category, updateCategory) = rememberSaveable { mutableStateOf("Books") }
    val categories = listOf(
        "Books",
        "Film",
        "Music",
        "Musicals & Theatre",
        "Television",
        "Video Games",
        "Board Games"
    )
    Column {
        Heading("Difficulty")
        RadioButtons(
            update = updateDifficulty,
            value = difficulty,
            options = listOf("Easy", "Medium", "Hard")
        )
        Heading("Category")
        RadioButtons(
            update = updateCategory,
            value = category,
            options = categories
        )
        Button(onClick = { startQuiz(difficulty, category)}) {
            Text("Start")
        }
    }
}

@Composable
fun RadioButtons(update: (String) -> Unit, value: String, options: List<String>) {
    options.forEach { text ->
        Row(
            Modifier
                .fillMaxWidth()
                .selectable(
                    selected = (text == value),
                    onClick = {
                        update(text)
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = (text == value),
                onClick = { update(text) }
            )
            Text(
                text = text,
                style = MaterialTheme.typography.body1.merge(),
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}

@Composable
fun Heading(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 20.dp),
    )
}


@Composable
fun AnswerQuestions(questions: List<Question>, finished: () -> Unit) {
    val (questionIndex, updateQuestionIndex) = rememberSaveable { mutableStateOf(0) }
    val question = questions[questionIndex]
    ShowQuestion(questionIndex, question) {
        if(questionIndex+1 == questions.size){
            finished()
        }
        updateQuestionIndex(questionIndex+1)
    }
}


@Composable
fun ShowQuestion(index: Int, question: Question, next: () -> Unit) {
    val answers = rememberSaveable{ (question.incorrectAnswers + question.correctAnswer).shuffled()}
    val (selectedAnswer, updateAnswer) = rememberSaveable { mutableStateOf(answers[0]) }
    val answerQuestion = rememberSaveable { mutableStateOf(true) }
    if(answerQuestion.value) {
        Column {
            Heading("Question ${index+1}")
            Text(question.question)
            RadioButtons(update = updateAnswer, value = selectedAnswer, options = answers)
            Button(onClick = {
                answerQuestion.value = false
            }) {
                Text("Answer")
            }
        }
    } else {
        Column {
            val text = if(selectedAnswer == question.correctAnswer) {
                "Correct"
            } else { "Wrong answer" }
            Text(text)
            Button(onClick = {
                answerQuestion.value = true
                next()
            }) {
                Text("Next")
            }
        }
    }
}