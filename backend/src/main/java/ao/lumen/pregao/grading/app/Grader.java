package ao.lumen.pregao.grading.app;

import com.fasterxml.jackson.databind.JsonNode;

interface Grader {
    String type();

    GradeResult grade(ExerciseDefinition def, JsonNode params, JsonNode answer);
}
