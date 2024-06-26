package io.immutables.lang.node;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface Term {
  @About(as = "EOF", kind = Kind.Other)
  short EOF = -1;

  @About(as = "_?_", kind = Kind.Other)
  short Unrecognized = 0;

  @About(as = "_whitespace_", kind = Kind.Other)
  short Whitespace = 100;

  @About(as = "\n", kind = Kind.Other)
  short Newline = 101;

  @About(as = "//", kind = Kind.Comment)
  short LineComment = 108;

  @About(as = "<name>", kind = Kind.Identifier)
  short Name = 1;
  @About(as = "<typename>", kind = Kind.Identifier)
  short Typename = 2;
  @About(as = "type", kind = Kind.Keyword)
  short Type = 3;

  @About(as = "concept", kind = Kind.Keyword)
  short Concept = 5;

  @About(as = "impl", kind = Kind.Keyword)
  short Impl = 6;

  @About(as = "tagx", kind = Kind.Keyword)
  short Tagx = 9;

  @About(as = "return", kind = Kind.Keyword)
  short Return = 10;

  @About(as = "if", kind = Kind.Keyword)
  short If = 11;

  @About(as = "else", kind = Kind.Keyword)
  short Else = 12;

  @About(as = "for", kind = Kind.Keyword)
  short For = 13;

  @About(as = "case", kind = Kind.Keyword)
  short Case = 14;

  @About(as = "true", kind = Kind.Keyword)
  short True = 16;

  @About(as = "false", kind = Kind.Keyword)
  short False = 17;

  @About(as = "<number>", kind = Kind.Other)
  short IntNumber = 20;

  @About(as = "<dec-number>", kind = Kind.Other)
  short DecNumber = 21;

  @About(as = "<exp-number>", kind = Kind.Other)
  short ExpNumber = 22;

  @About(as = "<bin-number>", kind = Kind.Other)
  short BinNumber = 23;

  @About(as = "<hex-number>", kind = Kind.Other)
  short HexNumber = 24;

  @About(as = "{", kind = Kind.Delimiter)
  short BraceL = 110;
  @About(as = "}", kind = Kind.Delimiter)
  short BraceR = 111;
  @About(as = "[", kind = Kind.DelimiterOrOperator)
  short BrackL = 112;
  @About(as = "]", kind = Kind.DelimiterOrOperator)
  short BrackR = 113;
  @About(as = "(", kind = Kind.Delimiter)
  short ParenL = 114;
  @About(as = ")", kind = Kind.Delimiter)
  short ParenR = 115;
  @About(as = ",", kind = Kind.Delimiter)
  short Comma = 116;
  @About(as = ".", kind = Kind.DelimiterOrOperator)
  short Dot = 117;

  @About(as = "\"", kind = Kind.Delimiter)
  short DoubleQuote = 120;
  @About(as = "'", kind = Kind.Delimiter)
  short SingleQuote = 121;
  @About(as = "`text`", kind = Kind.Markup)
  short Text = 128;

  @About(as = "|", kind = Kind.DelimiterOrOperator)
  short Pipe = 130;
  @About(as = "<", kind = Kind.DelimiterOrOperator)
  short Lower = 131;
  @About(as = ">", kind = Kind.DelimiterOrOperator)
  short Greater = 132;
  @About(as = "<=", kind = Kind.Operator)
  short LowerEquals = 133;
  @About(as = ">=", kind = Kind.Operator)
  short GreaterEquals = 134;

  @About(as = "=", kind = Kind.Operator)
  short Assign = 139;
  @About(as = "+", kind = Kind.Operator)
  short Plus = 140;
  @About(as = "-", kind = Kind.Operator)
  short Minus = 141;
  @About(as = "*", kind = Kind.Operator)
  short Multiply = 142;
  @About(as = "/", kind = Kind.Operator)
  short Divide = 143;
  @About(as = "%", kind = Kind.Operator)
  short Modulo = 144;
  @About(as = ":", kind = Kind.DelimiterOrOperator)
  short Colon = 145;
  @About(as = "?", kind = Kind.Operator)
  short Question = 146;
  @About(as = "~", kind = Kind.Operator)
  short Tilde = 147;
  @About(as = "&", kind = Kind.Operator)
  short Ampersand = 148;
  @About(as = "!", kind = Kind.Operator)
  short Exclaim = 149;

  @About(as = "::", kind = Kind.DelimiterOrOperator)
  short Quadot = 150;
  @About(as = "||", kind = Kind.Operator)
  short LogicOr = 151;
  @About(as = "&&", kind = Kind.Operator)
  short LogicAnd = 152;
  @About(as = "??", kind = Kind.Operator)
  short OrElse = 153;
  @About(as = "++", kind = Kind.Operator)
  short Increment = 154;
  @About(as = "--", kind = Kind.Operator)
  short Decrement = 155;
  @About(as = "==", kind = Kind.Operator)
  short Equals = 156;
  @About(as = "!=", kind = Kind.Operator)
  short NotEquals = 157;

  @About(as = "<-", kind = Kind.DelimiterOrOperator)
  short ArrowLeft = 158;
  @About(as = "->", kind = Kind.DelimiterOrOperator)
  short ArrowRight = 159;

  @About(as = "<>`tag-list-open`", kind = Kind.Markup)
  short TagListOpen = 160;
  @About(as = "`tag-list-close`</>", kind = Kind.Markup)
  short TagListClose = 161;
  @About(as = "<`tag-open`", kind = Kind.Markup)
  short TagOpen = 162;
  @About(as = "`tag-open-end`>", kind = Kind.Markup)
  short TagOpenEnd = 163;
  @About(as = "`tag-open-empty-end`/>", kind = Kind.Markup)
  short TagOpenEmptyEnd = 164;
  @About(as = "</`tag-close`", kind = Kind.Markup)
  short TagClose = 165;
  @About(as = "`tag-attr`", kind = Kind.Markup)
  short TagAttribute = 166;

  @About(as = "..", kind = Kind.Operator)
  short RangeInclusive = 170;
  @About(as = ">..", kind = Kind.Operator)
  short RangeExclusiveBegin = 171;
  @About(as = "..<", kind = Kind.Operator)
  short RangeExclusiveEnd = 172;

  @About(as = "+=", kind = Kind.Operator)
  short PlusAssign = 190;
  @About(as = "-=", kind = Kind.Operator)
  short MinusAssign = 191;
  @About(as = "*=", kind = Kind.Operator)
  short MultiplyAssign = 192;
  @About(as = "/=", kind = Kind.Operator)
  short DivideAssign = 193;
  @About(as = "%=", kind = Kind.Operator)
  short ModuloAssign = 194;
  @About(as = ":=", kind = Kind.Operator)
  short ColonAssign = 195;

  @About(as = "i8", kind = Kind.Keyword)
  short i8 = 200;
  @About(as = "u8", kind = Kind.Keyword)
  short u8 = 201;
  @About(as = "i16", kind = Kind.Keyword)
  short i16 = 202;
  @About(as = "u16", kind = Kind.Keyword)
  short u16 = 203;
  @About(as = "i32", kind = Kind.Keyword)
  short i32 = 204;
  @About(as = "u32", kind = Kind.Keyword)
  short u32 = 205;
  @About(as = "i64", kind = Kind.Keyword)
  short i64 = 206;
  @About(as = "u64", kind = Kind.Keyword)
  short u64 = 207;
  @About(as = "f32", kind = Kind.Keyword)
  short f32 = 208;
  @About(as = "f64", kind = Kind.Keyword)
  short f64 = 209;
  @About(as = "float", kind = Kind.Keyword)
  short Float = 210;
  @About(as = "int", kind = Kind.Keyword)
  short Int = 211;
  @About(as = "bool", kind = Kind.Keyword)
  short Bool = 212;

  enum Kind {
    Keyword, Identifier, Delimiter, Operator, DelimiterOrOperator, Comment, Markup, Other
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface About {
    String as() default "";
    Kind kind();
  }

  record AboutInfo(short code, String name, String symbol, Kind kind) {}

  TermsInfo Info = new TermsInfo(Term.class);
}
