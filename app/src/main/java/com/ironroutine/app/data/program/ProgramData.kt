package com.ironroutine.app.data.program

import com.ironroutine.app.data.db.PlanOverrideEntity
import java.time.DayOfWeek

enum class MeasureType { REPS, SECONDS }

enum class Equipment { DUMBBELL, BARBELL, BODYWEIGHT, PULLUP_BAR }

/** Описание упражнения (каталог). Чтобы добавить упражнение — см. README. */
data class ExerciseDef(
    val id: String,
    val name: String,
    val muscles: List<String>,
    val equipment: Equipment,
    /** true — в подходе записывается рабочий вес. */
    val usesWeight: Boolean,
    val measure: MeasureType = MeasureType.REPS,
    /** Например "на каждую руку" — если повторения считаются на одну сторону. */
    val perSide: String? = null,
    val shortTip: String
)

/** Упражнение в плане конкретного дня. */
data class PlannedExercise(
    val exerciseId: String,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val restMin: Int,
    val restMax: Int,
    val note: String? = null
)

data class WorkoutDay(
    val id: String,
    val weekday: DayOfWeek,
    val title: String,
    val subtitle: String,
    val exercises: List<PlannedExercise>
)

object ProgramData {

    private val list = listOf(
        ExerciseDef(
            id = "db_floor_press", name = "Жим гантелей лёжа на полу",
            muscles = listOf("Грудь", "Передние дельты", "Трицепс"),
            equipment = Equipment.DUMBBELL, usesWeight = true,
            shortTip = "Лопатки сведены, локти под углом около 45° к корпусу. Опускай гантели до касания пола локтями."
        ),
        ExerciseDef(
            id = "pushup", name = "Отжимания от пола",
            muscles = listOf("Грудь", "Трицепс", "Передние дельты", "Корпус"),
            equipment = Equipment.BODYWEIGHT, usesWeight = false,
            shortTip = "Тело — прямая линия от головы до пяток. Опускайся, пока грудь не окажется у пола."
        ),
        ExerciseDef(
            id = "db_ohp", name = "Жим гантелей над головой",
            muscles = listOf("Дельты", "Трицепс", "Верх груди"),
            equipment = Equipment.DUMBBELL, usesWeight = true,
            shortTip = "Пресс и ягодицы напряжены, поясница не прогибается. Жми вверх по лёгкой дуге."
        ),
        ExerciseDef(
            id = "lateral_raise", name = "Разведение гантелей в стороны",
            muscles = listOf("Средние дельты"),
            equipment = Equipment.DUMBBELL, usesWeight = true,
            shortTip = "Веди руки локтями до уровня плеч, без раскачивания. Опускай медленно."
        ),
        ExerciseDef(
            id = "overhead_tri_ext", name = "Разгибание гантели из-за головы",
            muscles = listOf("Трицепс"),
            equipment = Equipment.DUMBBELL, usesWeight = true,
            shortTip = "Локти направлены вверх и не разъезжаются. Работают только предплечья."
        ),
        ExerciseDef(
            id = "pullup", name = "Подтягивания на турнике",
            muscles = listOf("Широчайшие", "Бицепс", "Предплечья", "Корпус"),
            equipment = Equipment.PULLUP_BAR, usesWeight = false,
            shortTip = "Начинай движение с лопаток, подбородок выше перекладины. Оставляй 1–2 повторения в запасе."
        ),
        ExerciseDef(
            id = "bb_row", name = "Тяга штанги к животу в наклоне",
            muscles = listOf("Широчайшие", "Ромбовидные", "Задние дельты", "Бицепс"),
            equipment = Equipment.BARBELL, usesWeight = true,
            shortTip = "Спина прямая, наклон около 45°. Тяни штангу к низу живота, локти идут назад."
        ),
        ExerciseDef(
            id = "db_row", name = "Тяга гантели одной рукой",
            muscles = listOf("Широчайшие", "Задние дельты", "Бицепс"),
            equipment = Equipment.DUMBBELL, usesWeight = true, perSide = "на каждую руку",
            shortTip = "Тяни гантель к бедру, а не к плечу. Корпус не скручивается."
        ),
        ExerciseDef(
            id = "db_curl", name = "Сгибания рук с гантелями",
            muscles = listOf("Бицепс", "Плечевая мышца"),
            equipment = Equipment.DUMBBELL, usesWeight = true,
            shortTip = "Локти прижаты к корпусу, без раскачивания. Опускай гантели за 2–3 секунды."
        ),
        ExerciseDef(
            id = "hammer_curl", name = "Молотковые сгибания",
            muscles = listOf("Плечевая мышца", "Бицепс", "Предплечья"),
            equipment = Equipment.DUMBBELL, usesWeight = true,
            shortTip = "Ладони смотрят друг на друга. Локти неподвижны, корпус не качается."
        ),
        ExerciseDef(
            id = "wrist_curl", name = "Сгибания кистей с гантелями",
            muscles = listOf("Сгибатели запястья", "Предплечья"),
            equipment = Equipment.DUMBBELL, usesWeight = true,
            shortTip = "Предплечья лежат на бёдрах и не двигаются. Работают только кисти."
        ),
        ExerciseDef(
            id = "goblet_squat", name = "Приседания с гантелью",
            muscles = listOf("Квадрицепсы", "Ягодичные", "Корпус"),
            equipment = Equipment.DUMBBELL, usesWeight = true,
            shortTip = "Гантель у груди, спина прямая, колени идут по направлению носков."
        ),
        ExerciseDef(
            id = "rdl", name = "Румынская тяга со штангой",
            muscles = listOf("Задняя поверхность бедра", "Ягодичные", "Разгибатели спины"),
            equipment = Equipment.BARBELL, usesWeight = true,
            shortTip = "Отводи таз назад, штанга скользит вдоль ног. Спина остаётся прямой."
        ),
        ExerciseDef(
            id = "rev_lunge", name = "Выпады назад",
            muscles = listOf("Квадрицепсы", "Ягодичные"),
            equipment = Equipment.BODYWEIGHT, usesWeight = false, perSide = "на каждую ногу",
            shortTip = "Шаг назад, корпус прямой, переднее колено над стопой. Вернись усилием передней ноги."
        ),
        ExerciseDef(
            id = "hanging_knee_raise", name = "Подъём коленей в висе на турнике",
            muscles = listOf("Пресс", "Сгибатели бедра", "Предплечья"),
            equipment = Equipment.PULLUP_BAR, usesWeight = false,
            shortTip = "Поднимай колени к груди, слегка подкручивая таз. Без раскачивания."
        ),
        ExerciseDef(
            id = "plank", name = "Планка",
            muscles = listOf("Пресс", "Корпус", "Плечи", "Ягодичные"),
            equipment = Equipment.BODYWEIGHT, usesWeight = false, measure = MeasureType.SECONDS,
            shortTip = "Локти под плечами, тело — прямая линия. Дыши ровно, поясница не проседает."
        )
    )

    private val byId: Map<String, ExerciseDef> = list.associateBy { it.id }

    val allExercises: List<ExerciseDef> get() = list

    fun exerciseOrNull(id: String): ExerciseDef? = byId[id]

    fun exercise(id: String): ExerciseDef = byId.getValue(id)

    /**
     * Базовая программа. Здесь меняется расписание и состав тренировок.
     * Отдых: если в задании не указан, выставлено разумное значение (см. README).
     */
    val baseDays: List<WorkoutDay> = listOf(
        WorkoutDay(
            id = "push", weekday = DayOfWeek.MONDAY, title = "PUSH",
            subtitle = "Грудь, плечи, трицепс",
            exercises = listOf(
                PlannedExercise("db_floor_press", 3, 10, 15, 90, 120),
                PlannedExercise("pushup", 3, 8, 12, 60, 90),
                PlannedExercise("db_ohp", 2, 10, 12, 90, 90),
                PlannedExercise("lateral_raise", 3, 12, 15, 60, 90),
                PlannedExercise("overhead_tri_ext", 2, 10, 12, 60, 90)
            )
        ),
        WorkoutDay(
            id = "pull", weekday = DayOfWeek.WEDNESDAY, title = "PULL",
            subtitle = "Спина, бицепс, предплечья",
            exercises = listOf(
                PlannedExercise(
                    "pullup", 3, 1, 10, 90, 120,
                    note = "Максимум качественных повторений, не до полного отказа."
                ),
                PlannedExercise("bb_row", 3, 10, 12, 90, 120),
                PlannedExercise("db_row", 2, 12, 12, 60, 90),
                PlannedExercise("db_curl", 3, 10, 15, 60, 90),
                PlannedExercise("hammer_curl", 2, 12, 12, 60, 90),
                PlannedExercise("wrist_curl", 2, 15, 15, 45, 60)
            )
        ),
        WorkoutDay(
            id = "legs", weekday = DayOfWeek.FRIDAY, title = "LEGS + ABS",
            subtitle = "Ноги, пресс, дополнительная грудь",
            exercises = listOf(
                PlannedExercise("goblet_squat", 3, 12, 15, 90, 120),
                PlannedExercise("rdl", 3, 10, 12, 90, 120),
                PlannedExercise("rev_lunge", 2, 10, 10, 60, 90),
                PlannedExercise("hanging_knee_raise", 3, 10, 12, 60, 90),
                PlannedExercise("plank", 3, 30, 45, 45, 60),
                PlannedExercise("pushup", 2, 8, 12, 60, 90)
            )
        )
    )

    /** Применяет пользовательские изменения (подходы, повторения, отдых) из настроек. */
    fun applyOverrides(overrides: List<PlanOverrideEntity>): List<WorkoutDay> {
        if (overrides.isEmpty()) return baseDays
        val map = overrides.associateBy { it.dayId to it.exerciseId }
        return baseDays.map { day ->
            day.copy(exercises = day.exercises.map { pe ->
                val o = map[day.id to pe.exerciseId]
                if (o == null) pe else pe.copy(
                    sets = o.sets, repsMin = o.repsMin, repsMax = o.repsMax,
                    restMin = o.restMin, restMax = o.restMax
                )
            })
        }
    }
}
