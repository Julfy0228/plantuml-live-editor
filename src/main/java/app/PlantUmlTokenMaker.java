package app;

import javax.swing.text.Segment;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

public final class PlantUmlTokenMaker extends AbstractTokenMaker {
    @Override
    public TokenMap getWordsToHighlight() {
        TokenMap words = new TokenMap(true);
        for (String word : ("actor participant boundary control entity database collections queue "
                + "class interface abstract enum annotation object component package namespace node "
                + "cloud folder frame rectangle artifact storage agent usecase state start stop end "
                + "if then else elseif endif while endwhile repeat backward fork again split "
                + "note legend title caption header footer skinparam hide show remove "
                + "left right top bottom of over as is extends implements return activate deactivate "
                + "destroy create alt opt loop par break group end box ref autonumber "
                + "newpage together allowmixing scale direction partition detach kill "
                + "true false null public private protected").split(" ")) {
            words.put(word, Token.RESERVED_WORD);
        }
        return words;
    }

    @Override
    public String[] getLineCommentStartAndEnd(int languageIndex) {
        return new String[] {"'", null};
    }

    @Override
    public Token getTokenList(Segment segment, int initialTokenType, int startOffset) {
        resetTokenList();
        char[] chars = segment.array;
        int cursor = segment.offset;
        int limit = cursor + segment.count;
        int shift = startOffset - segment.offset;
        boolean blockComment = initialTokenType == Token.COMMENT_MULTILINE;
        while (cursor < limit) {
            int start = cursor;
            int type;
            char current = chars[cursor];
            if (blockComment || current == '/' && cursor + 1 < limit && chars[cursor + 1] == '\'') {
                if (!blockComment) {
                    cursor += 2;
                }
                blockComment = true;
                while (cursor < limit) {
                    if (chars[cursor] == '\'' && cursor + 1 < limit && chars[cursor + 1] == '/') {
                        cursor += 2;
                        blockComment = false;
                        break;
                    }
                    cursor++;
                }
                type = Token.COMMENT_MULTILINE;
            } else if (current == '\'') {
                cursor = limit;
                type = Token.COMMENT_EOL;
            } else if (Character.isWhitespace(current)) {
                while (cursor < limit && Character.isWhitespace(chars[cursor])) {
                    cursor++;
                }
                type = Token.WHITESPACE;
            } else if (current == '"') {
                cursor++;
                while (cursor < limit) {
                    char next = chars[cursor++];
                    if (next == '\\' && cursor < limit) {
                        cursor++;
                    } else if (next == '"') {
                        break;
                    }
                }
                type = Token.LITERAL_STRING_DOUBLE_QUOTE;
            } else if (Character.isLetter(current) || current == '_' || current == '@' || current == '!' || current == '$') {
                cursor++;
                while (cursor < limit && (Character.isLetterOrDigit(chars[cursor]) || chars[cursor] == '_')) {
                    cursor++;
                }
                if (current == '@' || current == '!') {
                    type = Token.PREPROCESSOR;
                } else if (current == '$') {
                    type = Token.VARIABLE;
                } else {
                    type = wordsToHighlight.get(chars, start, cursor - 1);
                    if (type < 0) {
                        type = Token.IDENTIFIER;
                    }
                }
            } else if (Character.isDigit(current) || current == '#') {
                cursor++;
                while (cursor < limit && (Character.isLetterOrDigit(chars[cursor]) || chars[cursor] == '.')) {
                    cursor++;
                }
                type = Token.LITERAL_NUMBER_DECIMAL_INT;
            } else {
                cursor++;
                type = "{}()[]".indexOf(current) >= 0 ? Token.SEPARATOR : Token.OPERATOR;
            }
            addToken(chars, start, cursor - 1, type, start + shift);
        }
        if (blockComment) {
            if (segment.count == 0) {
                addToken(chars, segment.offset, segment.offset - 1, Token.COMMENT_MULTILINE, startOffset);
            }
        } else {
            addNullToken();
        }
        return firstToken;
    }
}

