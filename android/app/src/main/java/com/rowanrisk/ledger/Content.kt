package com.rowanrisk.ledger

import java.time.LocalDateTime

data class Push(val quote: String, val author: String, val tool: String)

object Content {
    val items = listOf(
        Push("Discipline is choosing between what you want now and what you want most.", "Abraham Lincoln (attributed)", "Behavioral activation: do the smallest version of the task right now. Don't wait to feel like it."),
        Push("You do not rise to the level of your goals. You fall to the level of your systems.", "James Clear", "If-then plan: decide the trigger and the action in advance, so the decision is already made when the moment comes."),
        Push("Between stimulus and response there is a space. In that space is our power to choose our response.", "Viktor Frankl (attributed)", "Thought record: name the automatic thought, check it for all-or-nothing framing, then choose the next action anyway."),
        Push("Motivation is what gets you started. Habit is what keeps you going.", "Jim Ryun", "Graded exposure: break the task into a ladder and only move up a rung once the current one feels easy."),
        Push("We are what we repeatedly do. Excellence, then, is not an act, but a habit.", "Will Durant, summarizing Aristotle", "Values check: ask if you're avoiding this because of your mood right now, or because it's genuinely not worth doing."),
        Push("The pain of discipline weighs ounces. The pain of regret weighs tons.", "Jim Rohn (attributed)", "Implementation intention: \"If it's 0600, then I train before I check my phone.\" Write the exact line down."),
        Push("You don't have to see the whole staircase, just take the first step.", "Martin Luther King Jr. (attributed)", "Five-minute rule: commit to five minutes only. Most resistance dissolves once you're moving."),
        Push("Amateurs sit and wait for inspiration. The rest of us just get up and go to work.", "Stephen King, On Writing", "Behavioral activation: schedule the action at a fixed time regardless of mood, then log whether mood followed."),
        Push("Fatigue makes cowards of us all.", "Vince Lombardi", "Cognitive distortion check: is this unbearable, or just uncomfortable? Discomfort is data, not a stop sign."),
        Push("Suffering is optional. The intensity of your practice reveals your character.", "Common saying, author unknown", "Externalize the thought: write down the excuse. Excuses examined on paper lose most of their force."),
    )

    /** Two picks per day, the same split the web app uses (before and after 2 pm). */
    fun indexFor(now: LocalDateTime = LocalDateTime.now()): Int =
        (now.dayOfYear * 2 + if (now.hour >= 14) 1 else 0) % items.size
}
