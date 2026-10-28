package app;

import static org.junit.jupiter.api.Assertions.*;
import javax.swing.text.Segment;
import org.fife.ui.rsyntaxtextarea.Token;
import org.junit.jupiter.api.Test;

class PlantUmlTokenMakerTest {
    private Token lex(PlantUmlTokenMaker lexer, String source, int previous) {
        char[] chars = source.toCharArray();
        return lexer.getTokenList(new Segment(chars, 0, chars.length), previous, 0);
    }

    @Test
    void highlightsDirectivesKeywordsStringsAndUnicode() {
        PlantUmlTokenMaker lexer = new PlantUmlTokenMaker();
        assertEquals(Token.PREPROCESSOR, lex(lexer, "@startuml", Token.NULL).getType());
        assertEquals(Token.RESERVED_WORD, lex(lexer, "participant", Token.NULL).getType());
        assertEquals(Token.LITERAL_STRING_DOUBLE_QUOTE, lex(lexer, "\"Привет\"", Token.NULL).getType());
        assertEquals(Token.IDENTIFIER, lex(lexer, "Пользователь", Token.NULL).getType());
        assertEquals(Token.COMMENT_EOL, lex(lexer, "' комментарий", Token.NULL).getType());
    }

    @Test
    void preservesMultilineCommentsAndStopsAtClosingDelimiter() {
        PlantUmlTokenMaker lexer = new PlantUmlTokenMaker();
        Token opening = lex(lexer, "/' comment", Token.NULL);
        assertEquals(Token.COMMENT_MULTILINE, opening.getType());
        Token closing = lex(lexer, "comment '/ class", Token.COMMENT_MULTILINE);
        assertEquals(Token.COMMENT_MULTILINE, closing.getType());
        assertEquals(Token.RESERVED_WORD, closing.getNextToken().getNextToken().getType());
        assertEquals(Token.COMMENT_MULTILINE, lex(lexer, "", Token.COMMENT_MULTILINE).getType());
    }
}

