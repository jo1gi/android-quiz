package com.example.quiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quiz.ui.theme.QuizTheme
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.net.URLDecoder


enum class State {
    SelectQuiz,
    Loading,
    AnswerQuestions,
}

@Serializable
data class ApiResponse(
    val results: List<Question>
)

@Serializable
data class Question(
    val question: String,
    @SerialName("correct_answer")
    val correctAnswer: String,
    @SerialName("incorrect_answers")
    val incorrectAnswers: List<String>,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val (state, changeState) = rememberSaveable { mutableStateOf(State.SelectQuiz) }
            val (questions, changeQuestions) = rememberSaveable { mutableStateOf(listOf<Question>()) }
            QuizTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(all = 16.dp),
                    color = MaterialTheme.colors.background
                ) {
                    when (state) {
                        State.SelectQuiz -> {
                            SelectQuiz { difficulty, category ->
                                thread {
                                    loadQuestions(difficulty, category){ questions ->
                                        changeQuestions(questions)
                                        changeState(State.AnswerQuestions)
                                    }
                                }
                                changeState(State.Loading)
                            }
                        }
                        State.Loading -> {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("Loading")
                            }
                        }
                        State.AnswerQuestions -> {
                            AnswerQuestions(questions){ changeState(State.SelectQuiz) }
                        }
                    }
                }
            }
        }
    }
}

fun loadQuestions(difficulty: String, category: Int, setQuestions: (List<Question>) -> Unit) {
    val url = URL(
        "https://opentdb.com/api.php?amount=10&encode=url3986" +
                "&type=multiple" +
                "&difficulty=${difficulty.lowercase()}" +
                "&category=$category"
    )

    with(url.openConnection() as HttpURLConnection) {
        inputStream.bufferedReader().use {
            val response = it.readText()
            val json = Json{
                ignoreUnknownKeys = true
            }
            val jsonResponse = json.decodeFromString<ApiResponse>(response)
            setQuestions(jsonResponse.results)
        }
    }
}

@Composable
fun SelectQuiz(startQuiz: (String, Int) -> Unit) {
    val (difficulty, updateDifficulty) = rememberSaveable { mutableStateOf("Medium") }
    val categories = mapOf(
        "General Knowledge" to 9,
        "Books" to 10,
        "Film" to 11,
        "Music" to 12,
        "Musicals and Theatre" to 13,
        "Television" to 14,
        "Video Games" to 15,
        "Board Games" to 16,
        "Science & Nature" to 17,
        "Computers" to 18,
        "Mathematics" to 19,
        "Mythology" to 20,
        "Sports" to 21,
        "Geography" to 22,
        "History" to 23,
        "Politics" to 24,
        "Art" to 25,
        "Celebrities" to 26,
        "Animals" to 27,
        "Vehicles" to 28,
        "Comics" to 29,
        "Gadgets" to 30,
        "Anime & Manga" to 31,
        "Cartoon & Animation" to 32,
    )
    val (category, updateCategory) = rememberSaveable { mutableStateOf(categories.keys.first()) }
    Column(Modifier.verticalScroll(rememberScrollState())) {
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
            options = categories.keys.toList()
        )
        Button(onClick = { categories[category]?.let { startQuiz(difficulty, it) } }) {
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
    ShowQuestion(
        questionIndex,
        URLDecoder.decode(question.question, "utf-8"),
        URLDecoder.decode(question.correctAnswer, "utf-8"),
        answers = (question.incorrectAnswers + question.correctAnswer)
            .shuffled()
            .map { URLDecoder.decode(it, "utf-8")}
    ) {
        if(questionIndex+1 == questions.size){
            finished()
        }
        updateQuestionIndex(questionIndex+1)
    }
}


@Composable
fun ShowQuestion(
    index: Int,
    question: String,
    correctAnswer: String,
    answers: List<String>,
    next: () -> Unit
) {
    val (selectedAnswer, updateAnswer) = remember { mutableStateOf(answers[0]) }
    val answerQuestion = rememberSaveable { mutableStateOf(true) }
    if(answerQuestion.value) {
        Column {
            Heading("Question ${index+1}")
            Text(question)
            RadioButtons(
                update = updateAnswer,
                value = selectedAnswer,
                options = answers
            )
            Button(onClick = {
                answerQuestion.value = false
            }) {
                Text("Answer")
            }
        }
    } else {
        Column {
            val text = if(selectedAnswer == correctAnswer) {
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