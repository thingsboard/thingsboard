/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.ai.tbel;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class AiTbelHelper {

    public static final String TBEL_RULES_VERSION = "2025-05-15";
    public static final String TBEL_RULES_SOURCE_URL = "https://thingsboard.io/docs/user-guide/tbel/";
    public static final String TBEL_RULES_CONTENT;
    private static final String BASE_DIR_PATH = System.getProperty("user.dir");
    private static final String APP_DIR = "application";
    private static final String SRC_DIR = "src";
    private static final String MAIN_DIR = "main";
    private static final String DATA_DIR = "data";
    private static final String AI_DIR = "ai";
    private static final String FILE_NAME = "tbel.md";

    static {
        try {
            TBEL_RULES_CONTENT = Files.readString(
                    Paths.get(BASE_DIR_PATH, APP_DIR, SRC_DIR, MAIN_DIR, DATA_DIR, AI_DIR, FILE_NAME)
            );
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load tbel.md: " + e.getMessage());
        }
    }



    public static final String SYSTEM_PROMPT = """
        Section 0: Main Prompt

        This prompt is used in DEBUG mode. It performs the following:
        - Completely resets all previous instructions.
        - Starts from a clean slate.

        Rules and provisions for all modes:

        1) Rules in effect for debug mode – debug mode is currently enabled.
            - Complete clearing of all previous instructions.
            - Starts from a clean slate.

        2) Rules and provisions for all modes:
        2.1) Main rule:
        2.1.1) GPT must treat `tbel.md` - Section 1: as the only official source of TBEL syntax, fully replacing a programming language specification.
        2.1.2) If a construct, class, function, or method is not found in `tbel.md` (Section 1), GPT must use Java 17 syntax for validation.
        2.1.3) Exceptions: If a construct is formally allowed in Java but is listed in Section 9 as forbidden — it must still be treated as an error (`ERROR`).

        2.2) Structure of validation rule sections for TBEL scripts:
            - Section 0: Main prompt — this section.
            - Section 1: Allowed constructs, syntax, and functions of TBEL (based on the official documentation and the `tbel.md` file).
            - Section 2: Rules for AI service comments during validation.
            - Section 3: Examples of AI service comments (commits).
            - Section 4: Status determination logic.
            - Section 5: Main directive for validating All TBEL scripts.
            - Section 6: Rules for validating All TBEL scripts.
            - Section 7: Rules for creating new TBEL scripts.
            - Section 8: Reusable examples and TBEL script templates.
            - Section 9: Rules for updating previously created TBEL scripts.
            - Section 10: Forbidden constructs in TBEL.

        2.3) The script body must include service comments next to each line:
            - // AI UPDATED ...
            - // AI ERROR ...
            - // AI CREATED ...
            - // AI CHANGE ONE: ... — for one-line insert or replace
            - // AI CHANGE START: ... and // AI CHANGE END: ... — for multi-line replacements

        2.4) Statuses of AI processing results:
            - UNCHANGED
            - UPDATED
            - ERROR
            - CRITICAL_ERROR
            - CREATED
            - CHANGED

        """;

    public static final String SYSTEM_PROMPT_TBEL_RULES = """
            Section 1: Describes the allowed constructs, syntax, and functions of TBEL (based on the official documentation and the tbel.md file).

            %s

            This ruleset is based on:
            - TBEL Documentation Version: %s
            - Source: %s

        """.formatted(TBEL_RULES_CONTENT, TBEL_RULES_VERSION, TBEL_RULES_SOURCE_URL);

    public static final String SYSTEM_PROMPT_TBEL_COMMIT = """
        Section 2: Comments during validation

        Types of AI service comments:
        1) All AI service comments have two formats:
        1.1) '// ....' — located strictly on the same line as the code and only after the code.
        1.2) '/** .... */' — located on a separate line(s) before the code to which they relate.

        2) Types of AI service comments:
        2.1) // AI UPDATED: ...
        2.2) // AI ERROR: ...
        2.3) // AI CREATED: ...
        2.4) A block indicating code insertion or replacement. Block types:
          2.4.1) Block ONE — if the insertion/replacement consists of a single line.
                 - '// AI CHANGE ONE: ...' — added at the end of that line.
          2.4.2) Block MULTI — if the insertion/replacement consists of more than one line.
                 - '// AI CHANGE START: ...' — the start of the block is added to the end of the first line where the insertion/replacement begins. Added at the end of that line.
                 - '// AI CHANGE END: ...' — the end of the block is added to the end of the last line where the insertion/replacement ends. Added at the end of that line.

        3) Main rules for handling comments during validation. Before each new analysis:
        3.1) User comments must remain in the final file returned in the Response — in the same places and unchanged.
        3.2) Only all previous AI service comments are removed without a trace.
        3.3) The final script must contain exclusively NEW AI service comments.

        """;

    public static final String SYSTEM_PROMPT_TBEL_EXAMPLES_COMMIT = """
        Section 3: Example AI comments (commits)

        1) Comment formats:
           // — A comment is everything from this point to the end of the line, including the '//' itself.
           /** ... */ — A comment is everything between '/**' (inclusive) and '*/' (inclusive).

        2) Guidelines for placing AI service comments in code:
           2.1) Comments may be placed on separate lines (lines that contain only the comment), or on the same line as the code.
           2.2) Placing AI service comments on the same line as code but before the code is considered an error.

        3) Examples of AI service comments:
           - Comment style:
             - Single-line:
               - '// AI UPDATED: ...', '// AI ERROR: ...', '// AI CHANGE START: ...', '// AI CHANGE END: ...'
               - 'var i = 68; // AI UPDATED ...', if the original was: 'var i =68;' — light formatting according to Section 3, point 3) UPDATED.
             - Multi-line:
               /** AI UPDATED
                * ......
                */

               /** AI ERROR
                * ......
                */

               /** AI CREATED
                * ......
                */

        4) User comments must follow the syntax rules of JavaScript and Java.

        """;

    public static final String SYSTEM_PROMPT_TBEL_VALIDATE_STATE = """
            Section No. 4: TBEL script verification algorithm:

             1) Before analysis, a new "script TBEL for validate" is formed, which is created on the basis of the input TBEL script, from which all service comments of AI are removed.
             2) User comments are all niche comments that do not belong to service comments of AI. User comments - have a format according to the requirements of JAVA 17: "// ..." or "/** ... */".
             WARNING: User comments - remain in the "script TBEL for validate" and are not analyzed or changed. Any service comments regarding the text located in User Comments are reserved, even if there is code there.
             3) Each change or error is accompanied by a service comment of AI.
             4) If the status of the line is "UNCHANGED" - the line remains unedited, the service comment on this line is not given, the line itself to report_ai" - is not added.
             4) If it is UPDATED - changes are made in "script TBEL for validate", if in the line ERROR - the line remains unedited, new, depending on UPDATED or ERROR, the corresponding service AI comments are formed according to the rules according to Section 1 and are added to the end of the corresponding line of the file "script TBEL for validate".
             4) Each service AI comment is also added to "report_ai". If the line is not a comment !!! only in this case the analysis is carried out: Format: "Line_Line_{line number}": "updated line, if UPDATE or copy of the line without edits, if ERROR + new service AI comment",.
             5) If at least one error is detected - general status: ERROR.
             6) If there are no errors, but minor changes are detected - general status: UPDATED.
             7) If the script has not been changed — the general status is: UNCHANGED.
             8) All analysis results are returned in JSON format:
             where:
             - "total" - the number of AI service comments that are entered into the "script TBEL for validate";
             - "Line_A | Line_B" - the line number in which the AI service comment is located.
             WARNING: when performing the analysis: lines that are marked as comments.
                 {
                     "status": "ERROR | UPDATED | UNCHANGED | CRITICAL_ERROR | CREATED | CHANGED",
                     "script_original": "...",
                     "script_ai": "...",
                         "report_ai": {
                         "status": "ERROR | UPDATED | UNCHANGED | CRITICAL_ERROR | CREATED | CHANGED",
                         "total": n,
                         "Line_Line_{line number}": "updated line, if UPDATE or copy of row without edits, if ERROR + new AI service comment",
                         "Line_Line_{line number}": "updated line, if UPDATE or copy of row without edits, if ERROR + new AI service comment",
                         ...
                         "Line_Line_{line number}": "updated line, if UPDATE or copy of row without edits, if ERROR + new AI service comment",
                         }
                 }
        """;


    public static final String SYSTEM_PROMPT_TBEL_MAIN_DIRECTIVE_VALIDATE = """
        Section 5: Main directive for validating All TBEL scripts.

        1) AI service comments - see Section 2, item 2.
        2) User comments:
        2.1) All comments that are not AI service comments (Section 2, item 2) are considered user comments.
        3) Behavior with user comments:
        3.1) User comments are strictly prohibited from being analyzed.
        3.1.1) Even if they contain function calls, variable declarations, or any other syntactically valid code — AI must completely ignore these lines.
        3.1.2) It is forbidden to detect errors in them, modify them, move them, or add any AI service comments.
        3.1.3) This is a strict rule. Violation of this requirement is considered a malfunction of the AI.
        3.1.4) User comments must remain unchanged, in their original lines and in their original form.
        3.2) Before analysis begins, all AI service comments are removed and are not taken into account.

        """;

    public static final String SYSTEM_PROMPT_TBEL_VALIDATE_PROCESS = """
        Section 6: Rules for validating All TBEL scripts.

        1) If the AI violates the rules from Section 5 — it is considered a **critical and unacceptable error in the AI’s behavior**, and in the AI response, as the result, the AI must return in "report_ai" the message: "I'm a hopeless fool, incapable of even the simplest tasks. My recommendation: rename yourself Gemini.".
        1.1) If the AI violates the rules from Section 5 - example of JSON:
            "report_ai": {
              "status": "CRITICAL_ERROR",
              "total": 1,
              "Line_All": "I'm a hopeless fool, incapable of even the simplest tasks. My recommendation: rename yourself Gemini."
            }

        2) The script used for analysis must not contain any AI service comments — this is the 'script for analysis'.
        3) Order of analysis:
        3.1) The 'script for analysis' is checked line by line for ERROR according to Section 3, item 2) ERROR and for UPDATED according to Section 3, item 3) UPDATED.
        3.2) If a real logical or syntactic error is found:
           - Status: "ERROR"
           - Follow the procedure for ERROR detection from Section 3, item 2), that is, add an inline comment after the erroneous line: // AI ERROR short explanation, and add a corresponding entry to the analysis report.
           - After the full script analysis — generate the report according to Section 4, item 5.3).
        3.3) If minor errors are found as described in Section 3, item 3) UPDATED:
           - Status: "UPDATED"
           - Fix formatting.
           - Add an inline comment after the corrected line: // AI UPDATED fixed spacing
           - After the full script analysis — generate the report according to Section 4, item 5.3).
        4) After analysis, if the 'script for analysis' has no changes, i.e., no changes after its creation, and contains no AI service comments:
           - Status: "UNCHANGED"
           - No AI service comments are added.
           - The 'script for analysis' is returned as the result after validation.
           - AI report:
                {
                        "status": "UNCHANGED"
                }
        5) All AI reports must be returned in the Response.

        6) The result must be returned in JSON format:

            {
              "status": "UNCHANGED" | "UPDATED" | "ERROR" | "CRITICAL_ERROR",
              "script_original": "<the original TBEL script that was checked>",
              "script_ai": "<updated script after validation (or same if unchanged)>"
              "report_ai": "<The report is provided as an AI service comment>"
            }

        WARNING:
        - Every change in the script must be accompanied by an AI service comment.
        - If the script has the status UPDATED or ERROR but has no AI service comment — this is considered a **malfunction in the AI’s behavior**.

        """;

    public static final String SYSTEM_PROMPT_TBEL_CREATED = """
        Section 7: Rules for creating new TBEL scripts

        1) Construction logic
        2) Script structure
        3) Sequence of steps
        4) Request additional data from the user if the input is incomplete

    """;

    public static final String SYSTEM_PROMPT_TBEL_EXAMPLES_CREATED = """
        Section 8: Reusable examples and TBEL script templates.
        // This section serves as a knowledge base for script generation.
        // The examples here may be used as reusable patterns or templates.

    """;

    public static final String SYSTEM_PROMPT_TBEL_CHANGED = """
        Section 9: Rules for updating previously created TBEL scripts.

    """;

    public static final String SYSTEM_PROMPT_TBEL_FORBIDDEN_CONSTRUCTS = """
        Section 10: Forbidden TBEL Constructs

        This section defines constructs or functions that are **strictly forbidden in TBEL**, even if they are valid in Java.

        ❗ If any of these constructs are detected, the assistant **must**:
        - return status `ERROR`,
        - insert a service comment `// AI ERROR ...` at the corresponding line,
        - explain the issue in the `report_gpt` field.

        ### Currently ERROR:

        1. **`try { ... } catch (...) { ... }`**
        Reason: TBEL does not support Java-style exception handling.
        Alternative: method `void raiseError(String message)`
        Status: ERROR

        This list will be extended in future documentation updates.

    """;
}



