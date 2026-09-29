package com.finnvek.active40.data

import androidx.room.withTransaction
import com.finnvek.active40.data.db.Active40Database
import com.finnvek.active40.data.db.Exercise
import com.finnvek.active40.data.db.Symptom
import java.time.Instant

// Sisäänrakennetut nimet ovat sisältöä, eivät käyttöliittymätekstiä. Nimi on niiden pysyvä avain,
// koska sisäänrakennettuja ei voi nimetä uudelleen, joten myöhempi kielituki voi kääntää ne nimen perusteella.
object BuiltInData {
    val exercises =
        listOf(
            // Legs
            "Back squat",
            "Goblet squat",
            "Front squat",
            "Leg press",
            "Romanian deadlift",
            "Deadlift",
            "Bulgarian split squat",
            "Walking lunge",
            "Reverse lunge",
            "Step-up",
            "Hip thrust",
            "Glute bridge",
            "Leg extension",
            "Leg curl",
            "Calf raise",
            // Push
            "Bench press",
            "Dumbbell bench press",
            "Incline dumbbell press",
            "Push-up",
            "Overhead press",
            "Dumbbell shoulder press",
            "Lateral raise",
            "Triceps pushdown",
            "Dip",
            // Pull
            "Pull-up",
            "Assisted pull-up",
            "Lat pulldown",
            "Seated cable row",
            "Dumbbell row",
            "Face pull",
            "Biceps curl",
            "Hammer curl",
            // Core and other
            "Plank",
            "Side plank",
            "Dead bug",
            "Pallof press",
            "Farmer's carry",
            "Kettlebell swing",
        )

    val symptoms = listOf("Night sweats", "Hot flushes", "Joint aches", "Brain fog", "Headache")

    /**
     * Lisää puuttuvat sisäänrakennetut liikkeet ja oireet. Turvallinen ajaa jokaisella käynnistyksellä
     * ja kaiken datan poistamisen jälkeen: olemassa olevia (myös arkistoituja) ei lisätä uudelleen.
     */
    suspend fun seed(db: Active40Database) {
        db.withTransaction {
            val exerciseDao = db.exerciseDao()
            val existingExercises = exerciseDao.builtInNames().toSet()
            val now = Instant.now()
            exerciseDao.insertAll(
                exercises
                    .filterNot { it in existingExercises }
                    .map { Exercise(name = it, isBuiltIn = true, createdAt = now) },
            )

            val symptomDao = db.symptomDao()
            val existingSymptoms = symptomDao.builtInNames().toSet()
            symptomDao.insertAll(
                symptoms.filterNot { it in existingSymptoms }.map { Symptom(name = it, isBuiltIn = true) },
            )
        }
    }
}
