package io.immutables.lang.syntax;

import io.immutables.lang.Capacity;
import io.immutables.lang.Source;
import io.immutables.lang.node.Term;
import io.immutables.meta.Null;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unchecked") // constrained by DSL methods
public class GrammarVm implements Grammar, Grammar.Factory {
  // Opcodes (sort of IR) are defined as negative numbers to
  // easily distinguish codes from their parameters
  interface Opcode {
    byte DEFINE_ENTRY = -1;
    byte ASSUME_PRODUCTION = -2;
    byte ABSTRACT_PRODUCTION = -3;
    byte LABEL_PART = -5;
    byte MATCH_TERM = -6;
    byte MATCH_PRODUCTION = -7;
    byte UNINLINE_ENTRY = -8;
    byte TRY_NEXT = -9;
    byte DONE = -10;
    byte MATCH_NOT_TERM = -13;
    byte MATCH_SURE = -14;
    byte LAST_MATCHED = -15;
    byte MATCH_TERM_ONE_OF = -16;
  }

  private final IdentityHashMap<String, Short> literals = new IdentityHashMap<>();
  private short ignore = Short.MIN_VALUE;
  private Prod current;

  private final List<Prod> allProductions = new ArrayList<>();
  private final List<ProdPart> allParts = new ArrayList<>();
  // all used prods will start with index 1
  private final Prod UNUSED_PROD = new Prod();
  // all used parts will start with index 1
  private final ProdPart UNUSED_PART = new ProdPart(null);

  private final List<Runnable> defineLater = new ArrayList<>();

  private int[] codes = new int[128];
  private int codeCount;

  private final Class<?> umbrella;
  private final Class<?> termsHost;

  private GrammarVm(Class<?> umbrella, Class<?> termsHost) {
    this.umbrella = umbrella;
    this.termsHost = termsHost;
  }

  public static Factory factory(Class<?> umbrella, Class<?> termsHost) {
    return new GrammarVm(umbrella, termsHost);
  }

  public GrammarVm complete(Consumer<Grammar> define) {
    // very sketchy
    define.accept(this);

    if (allProductions.isEmpty() || codeCount == 0) {
      throw new AssertionError("No well-formed productions defined");
    }

    runDeferrerUntilNoMore();
    putCode(Opcode.DONE); // padding with additional DONE so that
    dereferenceProductionEntries();
    return this;
  }

  private void runDeferrerUntilNoMore() {
    // we iterate as callbacks may produce more defineLater callbacks
    // so we run until we exhaust all derived callbacks
    while (!defineLater.isEmpty()) {
      var snapshot = List.copyOf(defineLater);
      defineLater.clear();
      for (var r : snapshot) r.run();
    }
  }

  private void dereferenceProductionEntries() {
    int p = 0;
    while (p < codeCount) {
      if (codes[p++] == Opcode.MATCH_PRODUCTION) {
        // this replaces MATCH_PRODUCTION parameter pointing
        // to the index of production to the index of its entry in codes
        codes[p] = allProductions.get(codes[p]).entryCodeIndex;
        p += 2;
      }
    }
  }

  public void term(String literal, short term) {
    assert literal.intern() == literal : "must be interned literal";
    literals.put(literal, term);
  }

  public void ignore(short term) {
    ignore = term;
  }

  final class Prod implements
      Production<Object>,
      Grammar.Ephemeral<Object>,
      TermCapture<Object>,
      Alternatives<Object> {

    final int id;
    final @Null Object apply;
    final @Null Object complete;
    int entryCodeIndex;
    boolean isEphemeral;
    boolean isAbstract;
    @Null Prod uninlinedFrom;
    boolean isTerm;

    /*
        public int id() {
          return id;
        }
    */
    Prod() {
      this(null, null);
    }

    Prod(@Null Object apply) {
      this(apply, null);
    }

    Prod(@Null Object apply, @Null Object complete) {
      this.apply = apply;
      this.complete = complete;
      this.id = allProductions.size();
      allProductions.add(this);
    }

    public String toString() {
      return names().forProduction(id);
    }
  }

  final class ProdPart implements Part<Object, Object> {
    final int index;
    final BiConsumer<Object, Object> apply;

    ProdPart(BiConsumer<Object, Object> apply) {
      this.apply = apply;
      this.index = allParts.size();
      allParts.add(this);
    }

    public String toString() {
      return names().forPart(index);
    }
  }

  public <T> Production<T> production(Supplier<T> create) {
    assert create != null;
    return (Production<T>) new Prod(create);
  }

  public <T> Production<T> production(Function<Context, T> create) {
    assert create != null;
    return (Production<T>) new Prod(create);
  }

  public <T, F> Production<T> production(Function<Context, F> create, Function<F, T> complete) {
    assert create != null && complete != null;
    return (Production<T>) new Prod(create, complete);
  }

  public <T> Alternatives<T> alternatives() {
    var p = new Prod();
    p.isAbstract = true;
    return (Alternatives<T>) p;
  }

  public <T> Ephemeral<T> ephemeral() {
    var p = new Prod();
    p.isEphemeral = true;
    return (Ephemeral<T>) p;
  }

  public <T> TermCapture<T> termCapture(TermConsumer<T> apply) {
    var p = new Prod(apply);
    p.isEphemeral = true;
    p.isTerm = true;
    return (TermCapture<T>) p;
  }

  public <T, L> Part<T, L> part(BiConsumer<T, L> apply) {
    return (Part<T, L>) new ProdPart((BiConsumer<Object, Object>) apply);
  }

  public Grammar grammar() {
    return this;
  }

  private final Define defineImpl = new Define();

  private final class Define implements
      ProductionDefine<Object>,
      ProductionAlternative<Object>,
      AbstractProductionAlternative<Object> {

    public void is(Object... elements) {
      encodeAlternativeProduction(elements);
    }

    public ProductionAlternative<Object> or(Object... elements) {
      encodeAlternativeProduction(elements);
      return this;
    }

    public AbstractProductionAlternative<Object> or(NodeProduction<?> p) {
      assert p != null;
      encodeRedirectProduction((Prod) p);
      return this;
    }
  }

  private <T> ProductionDefine<T> uninlined(Prod p) {
    assert p.uninlinedFrom != null;
    current = p;
    current.entryCodeIndex = codeCount;
    putCode(Opcode.UNINLINE_ENTRY, current.id, p.uninlinedFrom.id);
    return (ProductionDefine<T>) defineImpl;
  }

  public <T> ProductionDefine<T> ephemeral(Ephemeral<T> e) {
    current = (Prod) e;
    // codeCount coincides with the next position at which next DEFINE_ENTRY
    // opcode will be inserted, so we know what will be the index of
    // code to enter the production IR
    current.entryCodeIndex = codeCount;
    putCode(Opcode.DEFINE_ENTRY, current.id);
    return (ProductionDefine<T>) defineImpl;
  }

  public <T> ProductionDefine<T> production(Production<T> p) {
    current = (Prod) p;
    current.entryCodeIndex = codeCount;
    putCode(Opcode.DEFINE_ENTRY, current.id);
    putCode(Opcode.ASSUME_PRODUCTION, current.id);
    return (ProductionDefine<T>) defineImpl;
  }

  public <T> AbstractProductionAlternative<T> production(Alternatives<T> a) {
    current = (Prod) a;
    current.entryCodeIndex = codeCount;
    putCode(Opcode.DEFINE_ENTRY, current.id);
    putCode(Opcode.ABSTRACT_PRODUCTION);
    return (AbstractProductionAlternative<T>) defineImpl;
  }

  private void encodeRedirectProduction(Prod production) {
    putCode(Opcode.TRY_NEXT, codeCount + 6); // +6 gives next index after these codes
    putCode(Opcode.MATCH_PRODUCTION, production.id, Grammar.ONLY_ONE);
    putCode(Opcode.DONE);
  }

  private void encodeAlternativeProduction(Object... elements) {
    assert elements.length > 0;

    putCode(Opcode.TRY_NEXT, 0); // this zero will be overridden by next offset
    int nextParameterPosition = codeCount - 1;

    for (var e : elements) {
      if (e instanceof Character character) {
        var literal = String.valueOf(character);
        short term = toTermCode(literal);
        putCode(Opcode.MATCH_TERM, term, Grammar.ONLY_ONE);
      } else if (e instanceof String literal) {
        short term = toTermCode(literal);
        putCode(Opcode.MATCH_TERM, term, Grammar.ONLY_ONE);
      } else if (e instanceof Short term) {
        putCode(Opcode.MATCH_TERM, term, Grammar.ONLY_ONE);
      } else if (e instanceof Prod production) {
        putCode(Opcode.MATCH_PRODUCTION, production.id, Grammar.ONLY_ONE);
      } else if (e instanceof MatchTerm match) {
        short term = toTermCode(match.term());
        int quantifier = match.quantifier();
        putCode(Opcode.MATCH_TERM, term, quantifier);
      } else if (e instanceof MatchNotTerm match) {
        short term = toTermCode(match.term());
        putCode(Opcode.MATCH_NOT_TERM, term);
      } else if (e instanceof MatchProduction match) {
        @Null var part = (ProdPart) match.part();
        int quantifier = match.quantifier();
        if (part != null) {
          putCode(Opcode.LABEL_PART, part.index);
        }
        var production = (Prod) match.production();

        putCode(Opcode.MATCH_PRODUCTION, production.id, quantifier);
      } else if (e instanceof MatchOneOfTerms match) {
        @Null var production = (Prod) match.production();
        Object[] terms = match.terms();
        assert terms.length > 0;
        // first argument is associated id to produce if match occurred
        // second argument is a number of trailing term codes
        putCode(Opcode.MATCH_TERM_ONE_OF, production != null ? production.id : 0, terms.length);
        // encode trailing list of terms, this is quite unique to this opcode
        // no need to tell that it should be consumed properly while reading.
        codes = Capacity.ensure(codes, codeCount, terms.length);
        for (var t : terms) codes[codeCount++] = toTermCode(t);
      } else if (e instanceof MatchInlineEphemeral match) {
        var production = uninlineProduction(match);

        putCode(Opcode.MATCH_PRODUCTION, production.id, match.quantifier());
      } else if (e instanceof LastMatched last) {
        short term = toTermCode(last.term());
        putCode(Opcode.LAST_MATCHED, term);
      } else if (e instanceof SureMatch m) {
        putCode(Opcode.MATCH_SURE);
      } else throw new AssertionError("Unsupported production element: " + e);
    }

    putCode(Opcode.DONE);
    codes[nextParameterPosition] = codeCount; // points to next offset after this alternative
  }

  private short toTermCode(String symbol) {
    @Null Short code = literals.get(symbol);
    if (code == null) throw new AssertionError("Unregistered literal: " + symbol);
    return code;
  }

  private short toTermCode(Object literal) {
    if (literal instanceof String symbol) {
      return toTermCode(symbol);
    }
    if (literal instanceof Short term) {
      return term;
    }
    throw new AssertionError("Unsupported term: " + literal);
  }

  private Prod uninlineProduction(MatchInlineEphemeral match) {
    var p = new Prod();
    p.isEphemeral = true;
    Prod host = current;
    while (host.uninlinedFrom != null) {
      host = host.uninlinedFrom;
    }
    p.uninlinedFrom = host;
    defineLater.add(() -> uninlined(p).is(match.elements()));
    return p;
  }

  private void putCode(int code) {
    assert code < 0;
    codes = Capacity.ensure(codes, codeCount, 1);
    codes[codeCount++] = code;
  }

  private void putCode(int code, int param) {
    assert code < 0 && param >= 0;
    codes = Capacity.ensure(codes, codeCount, 2);
    codes[codeCount++] = code;
    codes[codeCount++] = param;
  }

  private void putCode(int code, int param0, int param1) {
    assert code < 0 && param0 >= 0 && param1 >= 0;
    codes = Capacity.ensure(codes, codeCount, 3);
    codes[codeCount++] = code;
    codes[codeCount++] = param0;
    codes[codeCount++] = param1;
  }

  public final class Names {
    private final Map<Integer, String> forProductions = ConstantNames.discover(umbrella,
        ProductionKind.class::isAssignableFrom, o -> ((Prod) o).id);

    private final Map<Integer, String> forParts = ConstantNames.discover(umbrella,
        Part.class::isAssignableFrom, o -> ((ProdPart) o).index);

    private final Map<Integer, String> forCodes = ConstantNames.discover(Opcode.class);
    private final Map<Short, String> forTerms = ConstantNames.discoverTerms(termsHost);

    public String forProduction(int p) {
      return forProductions.getOrDefault(p, "#inline#" + p);
    }

    public String forCode(int c) {
      return forCodes.get(c);
    }

    public String forTerm(short t) {
      return forTerms.get(t);
    }

    public String forPart(int p) {
      return p == 0 ? "*" : forParts.getOrDefault(p, ":" + p);
    }
  }

  private @Null Names cachedNames;

  public Names names() {
    return cachedNames == null
        ? cachedNames = new Names()
        : cachedNames;
  }

  public String showCodes() {
    var names = names();

    var builder = new StringBuilder();
    int p = 0;
    while (p < codeCount) {
      builder.append(String.format("% 4d", p)).append("| ");
      int code = codes[p++];

      switch (code) {
        case Opcode.DEFINE_ENTRY:
        case Opcode.ASSUME_PRODUCTION:
          int prod = codes[p++];
          builder.append(names.forCode(code))
              .append(' ')
              .append(names.forProduction(prod));
          break;
        case Opcode.UNINLINE_ENTRY:
          int uninlined = codes[p++];
          int host = codes[p++];
          builder.append(names.forCode(code))
              .append(' ')
              .append(names.forProduction(uninlined))
              .append(" <= ")
              .append(names.forProduction(host));
          break;
        case Opcode.LABEL_PART:
          builder.append(names.forCode(code))
              .append(' ')
              .append(names.forPart(codes[p++]));
          break;
        case Opcode.MATCH_TERM:
          short term = (short) codes[p++];
          builder.append(names.forCode(code))
              .append(' ')
              .append(names.forTerm(term))
              .append(Show.showQuantifier(codes[p++]))
              .append(" #")
              .append(term);
          break;
        case Opcode.MATCH_NOT_TERM:
          short notTerm = (short) codes[p++];
          builder.append(names.forCode(code))
              .append(' ')
              .append(names.forTerm(notTerm))
              .append(" !")
              .append(notTerm);
          break;
        case Opcode.MATCH_PRODUCTION:
          int prodEntry = codes[p++]; // first parameter of MATCH_PRODUCTION
          int prodIndex = codes[prodEntry + 1]; // first parameter of DEFINE_ENTRY
          builder.append(names.forCode(code))
              .append(' ')
              .append(names.forProduction(prodIndex))
              .append(Show.showQuantifier(codes[p++]))
              .append(" :").append(prodEntry);
          break;
        case Opcode.MATCH_TERM_ONE_OF:
          int termProdIndex = codes[p++]; // first parameter
          int termCount = codes[p++]; // second parameter
          List<String> terms = new ArrayList<>();
          for (int i = 0; i < termCount; i++) {
            terms.add(names.forTerm((short) codes[p + i]));
          }
          p += termCount;
          builder.append(names.forCode(code))
              .append(' ')
              .append(termProdIndex == 0 ? "--" : names.forProduction(termProdIndex))
              .append(' ')
              .append(terms);
          break;
        case Opcode.TRY_NEXT:
          builder.append(names.forCode(code)).append(" :").append(codes[p++]);
          break;
        case Opcode.DONE, Opcode.ABSTRACT_PRODUCTION, Opcode.MATCH_SURE:
          builder.append(names.forCode(code));
          break;
        case Opcode.LAST_MATCHED:
          short lastTerm = (short) codes[p++];
          builder.append(names.forCode(code))
              .append(' ')
              .append(names.forTerm(lastTerm));
          break;
        default:
          throw new AssertionError("Unexpected opcode: " + code);
      }
      builder.append('\n');
    }
    return builder.toString();
  }

  public <T> Parsed<T> parse(Terms terms, Production<T> target) {
    var p = new Parser<T>(terms, (Prod) target);
    p.parse();
    return p;
  }

  public interface Parsed<T> {
    enum Outcome { Ok, Mismatched, Unconsumed, Undefined }
    Outcome outcome();
    boolean ok();
    Productions<T> productions();
    String message(Source.Lines lines, CharSequence source);
    T construct(Object context);
  }

  final class Parser<T> implements GrammarVm.Parsed<T> {
    private final Productions<T> result = new Productions<>();
    private final Terms terms;
    private final GrammarVm.Prod target;

    private int termLastMatched = -1;

    private int farthestMismatchTermIndex = -1; // not sure about term, let it be initially -1
    private int farthestMismatchWithinProduction = -1; // cannot be 0 production, but -1 to be
		// consistent
    private short farthestMismatchTermExpected;

    Outcome outcome = Outcome.Undefined;

    private boolean poisoned;

    Parser(Terms terms, GrammarVm.Prod target) {
      this.terms = terms;
      this.target = target;
    }

    public Outcome outcome() {
      return outcome;
    }

    void parse() {
      terms.rewind();
      // opportunistically use matchTerm to position to first non-ignored term
      matchTerm(Terms.SOI, PRODUCTION_UNSPECIFIED);

      boolean matched = matchProd(target.entryCodeIndex, PART_UNSPECIFIED);
      if (matched) {
        if (terms.current() == Terms.EOI) {
          outcome = Outcome.Ok;
        } else {
          outcome = Outcome.Unconsumed;
        }
      } else {
        outcome = Outcome.Mismatched;
      }
    }

    public boolean ok() {
      return outcome == Outcome.Ok;
    }

    public Productions<T> productions() {
      return result;
    }

    // TODO reset terms and productions properly
    private boolean matchProd(int entry, int part) {
      int p = entry;
      int withinProduction;

      if (codes[p] == Opcode.UNINLINE_ENTRY) {
        p++; // skip synthetic production id
        withinProduction = codes[++p];
        p++; // skip to next instruction
      } else {
        assert codes[p] == Opcode.DEFINE_ENTRY;
        withinProduction = codes[++p];
        p++; // skip to next instruction
      }

      int termOnEntry = terms.index;
      int termRangeBegin = terms.index();
      int indexOnEntry = result.index;
      int termLastMatchedOnEntry = termLastMatched;

      // these only have real values if ASSUME_PRODUCTION is used
      long assumedProductionWord = 0; // 0 - would be blank for no ASSUME_PRODUCTION defined
      int assumedProductionIndex = indexOnEntry;
      int defaultPartLabel = PART_UNSPECIFIED;

      if (codes[p] == Opcode.ASSUME_PRODUCTION) {
        int production = codes[++p]; // read production argument
        p++; // advance to next after ASSUME_PRODUCTION and its argument

        assumedProductionWord = Productions.encodeProductionPart(0L, production, part);
        assumedProductionIndex = result.put(assumedProductionWord);
      } else if (codes[p] == Opcode.ABSTRACT_PRODUCTION) {
        p++;
        defaultPartLabel = part;
      }
/*
			if (withinProduction == ((Prod) (Object) Lang.LiteralNumber).id) {
				System.out.println("DEBUG PROD!!!");
			}
*/
      boolean backtracks = true;

      tryNext:
      while (backtracks & codes[p] == Opcode.TRY_NEXT) {
        int next = codes[++p];
        p++;

        for (int labeledPart = defaultPartLabel; p < codeCount; ) {
          switch (codes[p++]) {
            case Opcode.DONE -> {
              if (assumedProductionWord > 0) {
                long w = assumedProductionWord;
                w = Productions.encodeTermRange(w, termRangeBegin, termLastMatched);
                w = Productions.encodeLength(w, result.index + 1 - assumedProductionIndex);
                result.set(assumedProductionIndex, w);
              }
              return true;
            }
            case Opcode.LABEL_PART -> {
              labeledPart = codes[p++];
            }
            case Opcode.MATCH_SURE -> {
              backtracks = false;
            }
            case Opcode.MATCH_PRODUCTION -> {
              int entryReference = codes[p++];
              int quantifier = codes[p++];

              boolean atLeastOne = quantifier % 2 == 1;
              boolean atMostOne = quantifier <= 1;

              for (boolean matched = false; ; ) {
                if (matchProd(entryReference, labeledPart)) {
                  if (atMostOne) break;
                  matched = true;
                } else if (poisoned) {
                  return false;
                } else if (atLeastOne & !matched) {
                  // failed to match at least one production
                  p = next;
                  result.index = assumedProductionIndex;
                  terms.index = termOnEntry;
                  termLastMatched = termLastMatchedOnEntry;
                  continue tryNext;
                } else {
                  // no more needed, consider match
                  break;
                }
              }
              // clear label after match
              labeledPart = defaultPartLabel;
            }
            case Opcode.MATCH_NOT_TERM -> {
              int term = codes[p++];
              backtrack:
              {
                int termIndex = terms.index();
                int termCurrent = terms.current();
                if (term == ignore) {
                  // if we match for NOT IGNORED term
                  // if we see that we've actually not ignored anything
                  // since last match, we declare our success and
                  // break out of backtrack
                  if (termLastMatched == --termIndex) break backtrack;
                } else {
                  // in case of non ignored term we do simple check
                  if (term != termCurrent) break backtrack;
                }
                // FIXME improper diagnostic, please get back to this and fix
                // in a meantime we've introduced MATCH_SURE opcode
                if (farthestMismatchTermIndex < termIndex) {
                  farthestMismatchTermIndex = termIndex;
                  farthestMismatchTermExpected = (short) term;
                  farthestMismatchWithinProduction = withinProduction;
                }

                p = next;
                result.index = assumedProductionIndex;
                terms.index = termOnEntry;
                termLastMatched = termLastMatchedOnEntry;
                continue tryNext;
              }
            }
            case Opcode.LAST_MATCHED -> {
              short term = (short) codes[p++];
              int last = terms.at(termLastMatched);
              if (last != term) {
                p = next;
                result.index = assumedProductionIndex;
                terms.index = termOnEntry;
                termLastMatched = termLastMatchedOnEntry;
                continue tryNext;
              }
            }
            case Opcode.MATCH_TERM_ONE_OF -> {
              int termProduction = codes[p++];
              int count = codes[p++];
              int termPointer = p;
              p += count;
              if (matchOneOfTerms(codes, termPointer, count, withinProduction)) {
                if (termProduction > 0) {
                  int term = terms.at(termLastMatched);
                  long w = Productions.encodeProductionPart(0, termProduction, term);
                  w = Productions.encodeTermRange(w, termRangeBegin, termLastMatched);
                  w = Productions.encodeLength(w, 1);
                  result.put(w); // put next child, not updating assumed production
                }
              } else {
                // failed to match at least one production
                p = next;
                result.index = assumedProductionIndex;
                terms.index = termOnEntry;
                termLastMatched = termLastMatchedOnEntry;
                continue tryNext;
              }
            }
            case Opcode.MATCH_TERM -> {
              int term = codes[p++];
              int quantifier = codes[p++];

              boolean atLeastOne = quantifier % 2 == 1;
              boolean atMostOne = quantifier <= 1;

              for (boolean matched = false; ; ) {
                if (matchTerm(term, withinProduction)) {
                  // matched and only one needed for success
                  if (atMostOne) break;
                  // may match some more
                  matched = true;
                } else if (atLeastOne & !matched) {
                  // failed to match at least one term
                  // continue to the next alternative if any
                  // as we've failed
                  p = next;
                  result.index = assumedProductionIndex;
                  terms.index = termOnEntry;
                  termLastMatched = termLastMatchedOnEntry;
                  continue tryNext;
                } else {
                  // when we don't match, but we don't require any
                  // more matches to proceed, break out of the loop
                  // with success match overall
                  break;
                }
              }
            }
            default -> throw new AssertionError("Illegal opcode sequence at " + (p - 1));
          }
        }
      }
      if (!backtracks) {
        poisoned = true;
        return false;
      }
      // match failed, reset terms and productions to allow for other alternatives
      result.index = indexOnEntry;
      terms.index = termOnEntry;
      termLastMatched = termLastMatchedOnEntry;
      return false;
    }

    private boolean matchOneOfTerms(int[] termsOneOf, int pointer, int count,
				int withinProduction) {
      int term = terms.current();
      for (int i = 0; i < count; i++) {
        int maybeMatchingTerm = termsOneOf[pointer + i];
        if (term == maybeMatchingTerm) {
          return termMatchConsumeIgnore();
        }
      }
      return termMismatch(term, withinProduction);
    }

    private boolean termMatchConsumeIgnore() {
      termLastMatched = terms.index();
      while (terms.next() == ignore) ;
      return true;
    }

    private boolean termMismatch(int term, int withinProduction) {
      int i = terms.index();
      if (farthestMismatchTermIndex < i) {
        farthestMismatchTermIndex = i;
        farthestMismatchTermExpected = (short) term;
        farthestMismatchWithinProduction = withinProduction;
      }
      return false;
    }

    private boolean matchTerm(int term, int withinProduction) {
      if (terms.current() == term) {
        return termMatchConsumeIgnore();
      } else {
        return termMismatch(term, withinProduction);
      }
    }

    public String message(Source.Lines lines, CharSequence source) {
      return switch (outcome) {
        case Ok -> "OK";
        case Undefined -> "not yet";
        case Mismatched -> buildMismatchedMessage(lines, source);
        case Unconsumed -> buildUnconsumedMessage(lines, source);
      };
    }

    @SuppressWarnings("unchecked") // should be safe by DSL construct
    public T construct(Object delegate) {
      assert ok();
      assert target.apply != null : "construction not defined for " + target;
      result.rewind();
      boolean shouldAdvance = result.advance();
      assert shouldAdvance : "should have at least one production available";
      var context = new Grammar.Context();
      context.delegate = delegate;
      var word = result.current();
      var prod = allProductions.get(Productions.decodeProduction(word));
      assert !prod.isEphemeral : "ephemeral production cannot construct";
      return (T) constructNode(word, prod, context);
    }

    private Object constructNode(long word, Prod prod, Grammar.Context context) {
      context.word = word;
      context.productionIndex = result.index;

      int stopAt = result.index + Productions.decodeLength(word);

      assert prod.apply != null : "construction not defined for " + prod;

      Object node = prod.apply instanceof Supplier
          ? ((Supplier<Object>) prod.apply).get()
          : ((Function<Context, Object>) prod.apply).apply(context);
      // advance only once per production, not inside loop or loop's condition,
      // otherwise recursion will not work
      if (result.advance()) {
        while (result.index() < stopAt) {
          // if we've not reached our stopAt point,
          // we're traversing node's children
          long u = result.current();
          int part = Productions.decodePart(u);
          // lookup production externally from child's constructNode
          // because we 've introduced special case TermProduction,
          // where different callback logic applies
          var childProd = allProductions.get(Productions.decodeProduction(u));

          if (childProd.isTerm) {
            short term = (short) part;
            assert term > 0 && Productions.decodeLength(u) == 1;
            var accept = (TermConsumer<Object>) childProd.apply;
            assert accept != null : "term consumer is not defined for " + prod;
            accept.accept(node, term);
            // as we don't recurse in this case, we have to call result.advance()
            // so loop will move to the next child if there are any
            if (result.advance()) continue;
            else break;
          }

          // it is part's callback is responsible to apply it
          // to parent production, but child node creation callback
          // can have side effects, so it make sense to continue calling those
          Object child = constructNode(u, childProd, context);
          if (part > 0) {
            allParts.get(part).apply.accept(node, child);
          }
        }
      }
      // postprocessing using `complete` function if it is provided
      // which can change or even replace the resulting node
      if (prod.complete instanceof Function<?, ?>) {
        node = ((Function<Object, Object>) prod.complete).apply(node);
      }
      return node;
    }

    private String buildUnconsumedMessage(Source.Lines lines, CharSequence source) {
      int i = farthestMismatchTermIndex;
      Source.Range range = Source.Range.of(
          lines.get(terms.sourcePositionBefore(i)),
          lines.get(terms.sourcePositionAfter(i)));

      return range.begin
          + " Unexpected terms starting with `" + range.get(source) + "` "
          + "\n\t" + Source.Excerpt.from(source, lines).get(range).toString().replace("\n", "\n\t")
          + "Unconsumed terms which are not forming any construct";
    }

    private String buildMismatchedMessage(Source.Lines lines, CharSequence source) {
      assert farthestMismatchTermIndex >= 0
          && farthestMismatchWithinProduction > PRODUCTION_UNSPECIFIED;
      var names = names();

      int mismatchTerm = terms.at(farthestMismatchTermIndex);
      int positionBefore = terms.sourcePositionBefore(farthestMismatchTermIndex);
      int positionAfter = terms.sourcePositionAfter(farthestMismatchTermIndex);
      if (positionAfter < positionBefore) positionAfter = positionBefore;

      Source.Range range = Source.Range.of(
          lines.get(positionBefore),
          lines.get(positionAfter));

      CharSequence rangeText = range.get(source);

      return range.begin
          + " Stumbled on `" + rangeText + "`"// + Term.Info.about(mismatchTerm).symbol()
          + " while expecting " + Term.Info.about(farthestMismatchTermExpected).symbol().replace(
							"\n", "\\n")
          + " term in/after " + names.forProduction(farthestMismatchWithinProduction) + ""
          + "\n\t" + Source.Excerpt.from(source, lines).get(range).toString().replace("\n", "\n\t")
          + "Cannot parse production because of mismatched term";
    }
  }

  private static final int PRODUCTION_UNSPECIFIED = 0;
  private static final int PART_UNSPECIFIED = 0;
}
