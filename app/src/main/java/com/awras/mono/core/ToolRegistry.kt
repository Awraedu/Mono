package com.awras.mono.core

import com.awras.mono.features.tools.audio.audioTools
import com.awras.mono.features.tools.datetime.dateTimeTools
import com.awras.mono.features.tools.education.educationTools
import com.awras.mono.features.tools.files.filesTools
import com.awras.mono.features.tools.finance.financeTools
import com.awras.mono.features.tools.games.gamesTools
import com.awras.mono.features.tools.maths.mathsTools
import com.awras.mono.features.tools.measurement.measurementTools
import com.awras.mono.features.tools.productivity.productivityTools
import com.awras.mono.features.tools.travel.travelTools
import com.awras.mono.features.tools.vision.visionTools

object ToolRegistry {
    val allTools = measurementTools +
            visionTools +
            audioTools +
            travelTools +
            productivityTools +
            mathsTools +
            filesTools +
            financeTools +
            dateTimeTools +
            educationTools +
            gamesTools
}