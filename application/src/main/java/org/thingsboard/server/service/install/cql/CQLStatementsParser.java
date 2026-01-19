/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.install.cql;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CQLStatementsParser {

    enum State {
        DEFAULT,
        INSINGLELINECOMMENT,
        INMULTILINECOMMENT,
        INQUOTESTRING,
        INSQUOTESTRING,

    }

    private String text;
    private State state;
    private int pos;
    private List<String> statements;

    public CQLStatementsParser(Path cql) throws IOException {
        try {
            List<String> lines = Files.readAllLines(cql);
            StringBuilder t = new StringBuilder();
            for (String l : lines) {
                t.append(l.trim());
                t.append('\n');
            }

            text = t.toString();
            pos = 0;
            state = State.DEFAULT;
            parseStatements();
        }
        catch (IOException e) {
            log.error("Unable to parse CQL file [{}]!", cql);
            log.error("Exception", e);
            throw e;
        }
    }

    public List<String> getStatements() {
        return this.statements;
    }

    private void parseStatements() {
        this.statements = new ArrayList<>();
        StringBuilder statementUnderConstruction = new StringBuilder();

        char c;
        while ((c = getChar()) != 0) {
            switch (state) {
                case DEFAULT:
                    processDefaultState(c, statementUnderConstruction);
                    break;
                case INSINGLELINECOMMENT:
                    if (c == '\n') {
                        state = State.DEFAULT;
                    }
                    break;

                case INMULTILINECOMMENT:
                    if (c == '*' && peekAhead() == '/') {
                        state = State.DEFAULT;
                        advance();
                    }
                    break;

                case INQUOTESTRING:
                    processInQuoteStringState(c, statementUnderConstruction);
                    break;
                case INSQUOTESTRING:
                    processInSQuoteStringState(c, statementUnderConstruction);
                    break;
            }

        }
        String tmp = statementUnderConstruction.toString().trim();
        if (tmp.length() > 0) {
            this.statements.add(tmp);
        }
    }

    private void processDefaultState(char c, StringBuilder statementUnderConstruction) {
        if ((c == '/' && peekAhead() == '/') || (c == '-' && peekAhead() == '-')) {
            state = State.INSINGLELINECOMMENT;
            advance();
        } else if (c == '/' && peekAhead() == '*') {
            state = State.INMULTILINECOMMENT;
            advance();
        } else if (c == '\n') {
            statementUnderConstruction.append(' ');
        } else {
            statementUnderConstruction.append(c);
            if (c == '\"') {
                state = State.INQUOTESTRING;
            } else if (c == '\'') {
                state = State.INSQUOTESTRING;
            } else if (c == ';') {
                statements.add(statementUnderConstruction.toString().trim());
                statementUnderConstruction.setLength(0);
            }
        }
    }

    private void processInQuoteStringState(char c, StringBuilder statementUnderConstruction) {
        statementUnderConstruction.append(c);
        if (c == '"') {
            if (peekAhead() == '"') {
                statementUnderConstruction.append(getChar());
            } else {
                state = State.DEFAULT;
            }
        }
    }

    private void processInSQuoteStringState(char c, StringBuilder statementUnderConstruction) {
        statementUnderConstruction.append(c);
        if (c == '\'') {
            if (peekAhead() == '\'') {
                statementUnderConstruction.append(getChar());
            } else {
                state = State.DEFAULT;
            }
        }
    }

    private char getChar() {
        if (pos < text.length())
            return text.charAt(pos++);
        else
            return 0;
    }

    private char peekAhead() {
        if (pos < text.length())
            return text.charAt(pos);  // don't advance
        else
            return 0;
    }

    private void advance() {
        pos++;
    }

}
