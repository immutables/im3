package io.immutables.lang.syntax;

import io.immutables.lang.node.Identifier;
import io.immutables.lang.node.Node;
import io.immutables.lang.node.SourceSpan;
import io.immutables.lang.node.Term;
import java.util.ArrayList;
import java.util.List;
import static io.immutables.lang.syntax.Grammar.*;

public interface Lang {
  Grammar.Factory f = GrammarVm.factory(Lang.class, Term.class);

  Production<Node.Unit> Unit = f.production(Node.Unit::new);
  Part<Node.Unit, Node.Element> element = f.part((u, e) -> u.elements.add(e));
  Alternatives<Node.Element> Element = f.alternatives();

  // 2 for leading '//', -3 for leading '//' and trailing '\n'
  Production<SourceSpan> LineComment = f.production(c ->
      ForLang.span(c).nudge(2, -3));

  Part<Node.CommentedOn, SourceSpan> comment = f.part((c, s) -> c.comment.add(s));
  Production<Node.Comments> Comments = f.production(Node.Comments::new);
  Production<Node.Blanks> Blanks = f.production(() -> Node.Blanks);

  Production<Node.TypeDeclaration> TypeDeclaration = f.production(Node.TypeDeclaration::new);
  Part<Node.TypeDeclaration, Node.ParametersOrConstructors>
      shape = f.part((t, p) -> t.shape = p);
  Part<Node.TypeDeclaration, Node.ImplFeatures> implFeatures = f.part((t, impl) -> t.impl = impl);
  Part<Node.ImplFeatures, Node.Element> implElement = f.part((u, e) -> u.elements.add(e));
  Alternatives<Node.Element> ImplElement = f.alternatives();

  Production<Node.ImplFeatures> ImplFeatures = f.production(Node.ImplFeatures::new);

  Part<Node.NamedDeclaration, Identifier> name = f.part((t, i) -> t.name = i);

  Production<Node.ValueBinding> ValueBinding = f.production(Node.ValueBinding::new);
  Part<Node.ValueBinding, Node.Expression> bindingValue = f.part((b, e) -> b.value = e);

  Alternatives<Node.ParametersOrConstructors> ParametersOrConstructors = f.alternatives();
  Production<Node.ConstructorCases> ConstructorCases = f.production(Node.ConstructorCases::new);

  Alternatives<Node.Parameters> Parameters = f.alternatives();

  Production<Node.ParameterProduct> ParameterProduct = f.production(Node.ParameterProduct::new);

  Part<Node.ParameterProduct, Node.ParameterProductComponent> component =
      f.part((p, c) -> p.components.add(c));

  Production<Node.ParameterRecord> ParameterRecord = f.production(Node.ParameterRecord::new);

  Part<Node.ParameterRecord, Node.ParameterNamedGroup>
      field = f.part((p, c) -> p.fields.add(c));

  Alternatives<Node.ParameterProductComponent> ParameterProductComponent = f.alternatives();
  Part<Node.ParameterProductComponent, Node.TypeUse> ptype = f.part((p, t) -> p.type = t);
  Part<Node.ParameterProductComponent, Node.Expression>
      pdefault = f.part((p, e) -> p.defaultValue = e);

  Production<Node.ParameterUnnamed> ParameterUnnamed = f.production(Node.ParameterUnnamed::new);

  Production<Node.ParameterNamedGroup>
      ParameterNamedGroup = f.production(Node.ParameterNamedGroup::new);
  Part<Node.ParameterNamedGroup, Identifier> pname = f.part((g, n) -> g.names.add(n));

  Alternatives<Node.TypeUse> TypeUse = f.alternatives();

  Alternatives<Node.TypeInvariant> TypeInvariant = f.alternatives();
  Alternatives<Node.TypeInvariant> TypeUnambiguous = f.alternatives();
  Production<Node.TypeVariant> TypeVariant = f.production(Node.TypeVariant::new);
  Production<Node.TypeNominal> TypeNominal = f.production(Node.TypeNominal::new);
  Part<Node.TypeNominal, Identifier> tname = f.part((t, n) -> t.name = n);
  Part<Node.TypeNominal, List<Node.TypeUse>> typeArguments = f.part((t, a) -> t.arguments = a);
  Production<Node.TypeEmpty> TypeEmpty = f.production(Node.TypeEmpty::new);

  Ephemeral<Void> parameterSeparator = f.ephemeral();
  Ephemeral<Void> beginningOfStatements = f.ephemeral();

  Production<List<Node.TypeUse>> TypeArguments = f.production(() -> new ArrayList<>());
  Part<List<Node.TypeUse>, Node.TypeUse> argument = f.part(List::add);

  Production<Identifier> Name = f.production(ForLang::identifer);
  Production<Identifier> TagAttributeName = f.production(ForLang::identifer);
  Production<Identifier> Typename = f.production(ForLang::identifer);

  Production<Node.TypeContainer> TypeArray =
      f.production(c -> Node.TypeContainer.kind(Node.TypeContainer.Kind.Array));

  Part<Node.TypeContainer, Node.TypeUse> content = f.part((t, a) -> t.content = a);
  Production<Node.TypeContainer> TypeOptional =
      f.production(c -> Node.TypeContainer.kind(Node.TypeContainer.Kind.Optional));
  Production<Node.TypeContainer> TypeSlot =
      f.production(c -> Node.TypeContainer.kind(Node.TypeContainer.Kind.Slot));

  Production<Node.FeatureDeclaration>
      FeatureDeclaration = f.production(Node.FeatureDeclaration::new);

  Ephemeral<Node.FeatureDeclaration> FeatureDeclarationTrail = f.ephemeral();

  Part<Node.FeatureDeclaration, Node.Parameters> input = f.part((f, i) -> f.input = i);

  Part<Node.FeatureDeclaration, Node.TypeUse> output = f.part((f, o) -> f.output = o);

  Part<Node.FeatureDeclaration, Node.Statements> statements = f.part((f, s) -> f.statements = s);

  Production<Node.Statements>
      Statements = f.production(c -> new Node.Statements(), Node.Statements::markLast);

  Ephemeral<Node.Statements> StatementsElement = f.ephemeral();

  Part<Node.Statements, Node.StatementsElement> statement = f.part((s, e) -> s.statements.add(e));

  Production<Node.ReturnStatement>
      ReturnStatement = f.production(c -> new Node.ReturnStatement());
  Production<Node.StandaloneExpression>
      StandaloneExpression = f.production(Node.StandaloneExpression::new);
  Part<Node.ReturnStatement, Node.Expression> returned = f.part((r, e) -> r.value = e);
  Part<Node.StandaloneExpression, Node.Expression> expression = f.part((r, e) -> r.value = e);

  Production<Node.LocalBinding> LocalBinding = f.production(Node.LocalBinding::new);
  TermCapture<Node.LocalBinding> slot = f.termCapture((lb, t) -> lb.slot());
  /*	Production<Node.LocalBinding>
      LocalSlotBinding = f.production(c -> new Node.LocalBinding().slot());*/
  Part<Node.LocalBinding, Identifier> localName = f.part((b, n) -> b.name = n);
  Part<Node.LocalBinding, Node.Expression> localValue = f.part((b, e) -> b.value = e);

  Production<Node.LocalMultiBinding> LocalMultiBinding = f.production(Node.LocalMultiBinding::new);
  Part<Node.LocalMultiBinding, Identifier> localNames = f.part((b, n) -> b.names.add(n));
  Part<Node.LocalMultiBinding, Node.Expression> localValues = f.part((b, e) -> b.values.add(e));

  Alternatives<Node.Expression> Expression = f.alternatives();
  Production<Node.Expression>
      OperatorSoup = f.production(c -> new Node.OperatorSoup(), Node.OperatorSoup::fold);
  Ephemeral<Node.OperatorSoup> OperatorBroth = f.ephemeral();
  TermCapture<Node.OperatorSoup> operator = f.termCapture(Node.OperatorSoup::binaryOperator);
  TermCapture<Node.OperatorSoup> assignOperator = f.termCapture(Node.OperatorSoup::assignOperator);
  TermCapture<Node.OperatorSoup> prefixOperator = f.termCapture(Node.OperatorSoup::prefixOperator);
  TermCapture<Node.OperatorSoup> suffixOperator = f.termCapture(Node.OperatorSoup::suffixOperator);
  Part<Node.OperatorSoup, Node.Expression> operand = f.part(Node.OperatorSoup::operand);

  Production<Node.Expression>
      FeatureExpression = f.production(c -> new Node.ApplyChain(), Node.ApplyChain::fold);
  Ephemeral<Node.ApplyChain> FeatureSelectOrApply = f.ephemeral();
  Part<Node.ApplyChain, Node.Expression> base = f.part((t, b) -> t.base = b);
  Part<Node.ApplyChain, Node.Expression> apply = f.part(Node.ApplyChain::apply);
  TermCapture<Node.ApplyChain> option = f.termCapture(Node.ApplyChain::option);
  Part<Node.ApplyChain, Identifier> select = f.part(Node.ApplyChain::select);

  Alternatives<Node.Expression> FeatureInput = f.alternatives();
  Alternatives<Node.Expression> BaseExpression = f.alternatives();
  Production<Node.Reference> Reference = f.production(Node.Reference::new);
  Production<Node.Reference> TypeReference = f.production(Node.Reference::new);
  Part<Node.Reference, Identifier> referenceName = f.part((r, n) -> r.name = n);

  Production<Node.Expression> LiteralEmpty = f.production(Node.LiteralEmpty::new);
  Production<Node.Expression>
      LiteralProduct = f.production(c -> new Node.LiteralProduct(), Node.LiteralProduct::fold);
  Production<Node.LiteralRecord> LiteralRecord = f.production(Node.LiteralRecord::new);
  Production<Node.LiteralArray> LiteralArray = f.production(Node.LiteralArray::new);
  Part<Node.DelimitedComponents, Node.Expression>
      componentExpression = f.part((d, e) -> d.components.add(e));
  Part<Node.LiteralRecord, Identifier> fieldName = f.part((d, n) -> d.names.add(n));
  Production<Node.ConstructorApply> ConstructorApply = f.production(Node.ConstructorApply::new);
  Part<Node.ConstructorApply, Node.TypeNominal> constructor = f.part((c, t) -> c.type = t);
  Part<Node.ConstructorApply, Identifier> constructorCase = f.part((c, n) -> c.alternative = n);
  Part<Node.ConstructorApply, Node.Expression> constructorInput = f.part((t, a) -> t.input = a);

  Production<Node.LambdaExpression> LambdaExpression = f.production(Node.LambdaExpression::new);
  Part<Node.LambdaExpression, Node.Parameters> lambdaParameter = f.part((f, i) -> f.input = i);
  Part<Node.LambdaExpression, Node.ExpressionOrStatements>
      lambdaBody = f.part((f, e) -> f.body = e);
  Production<Node.LambdaOperatorExpression>
      LambdaOperatorExpression = f.production(Node.LambdaOperatorExpression::new);
  TermCapture<Node.LambdaOperatorExpression>
      lambdaOperator = f.termCapture(Node.LambdaOperatorExpression::operator);

  Production<SourceSpan> LiteralText = f.production(ForLang::span);
  Alternatives<Node.LiteralMarkup> LiteralMarkup = f.alternatives();
  Production<Node.LiteralString> LiteralString = f.production(Node.LiteralString::new);
  Ephemeral<Node.LiteralString> LiteralStringElement = f.ephemeral();
  Production<Node.LiteralTag> LiteralTag = f.production(Node.LiteralTag::new);
  Production<Node.LiteralTagList> LiteralTagList =
      f.production(Node.LiteralTagList::new);
  Part<Node.LiteralWithText, Node.TextElement> text = f.part((m, t) -> m.elements.add(t));
  Part<Node.LiteralTag, Identifier> tagName = f.part((m, n) -> m.name = n);

  Production<Identifier> TagName = f.production(c -> ForLang.identifer(c, 1, 0)); // was 1, -1 ???
  Ephemeral<Node.LiteralTag> TagClosing = f.ephemeral();
  Ephemeral<Node.LiteralTag> TagAttribute = f.ephemeral();
  Ephemeral<Node.LiteralTag> TagAttributeValue = f.ephemeral();
  Ephemeral<Node.LiteralWithText> TagNestedElement = f.ephemeral();
  TermCapture<Node.LiteralTag> emptyTag = f.termCapture((tag, term) -> tag.isEmpty = true);

  Part<Node.LiteralTag, Identifier> attributeName =
      f.part((t, n) -> t.attributeNames.add(n));
  Part<Node.LiteralTag, Node.Expression> attributeValue =
      f.part((t, e) -> t.attributeValues.add(e));

  Production<Node.LiteralNumber>
      LiteralNumber = f.production(c -> Node.LiteralNumber.number(ForLang.span(c)));
  TermCapture<Node.LiteralNumber> numberKind = f.termCapture(Node.LiteralNumber::fromTerm);
  Production<Node.LiteralBoolean> LiteralBoolean = f.production(Node.LiteralBoolean::new);
  TermCapture<Node.LiteralBoolean> booleanValue = f.termCapture(Node.LiteralBoolean::fromTerm);

  Alternatives<Node.ExpressionOrStatements> ExpressionOrStatements = f.alternatives();
  Production<Node.IfStatement> IfStatement = f.production(Node.IfStatement::new);
  Part<Node.IfStatement, Node.Expression> condition = f.part((ifs, e) -> ifs.condition = e);
  Part<Node.IfStatement, Node.ExpressionOrStatements> then = f.part((ifs, e) -> ifs.then = e);
  Part<Node.IfStatement, Node.ExpressionOrStatements> otherwise =
			f.part((ifs, e) -> ifs.otherwise = e);
  Production<Node.ForStatement> ForStatement = f.production(Node.ForStatement::new);
  Part<Node.ForStatement, Identifier> bindFor = f.part((fors, e) -> fors.bind.add(e));
  Part<Node.ForStatement, Node.Expression> iterable = f.part((fors, e) -> fors.iterable = e);
  Part<Node.ForStatement, Node.ExpressionOrStatements> body = f.part((fors, e) -> fors.loop = e);
  Production<Node.CaseStatement> CaseStatement = f.production(Node.CaseStatement::new);
  Part<Node.CaseStatement, Node.Expression> matched = f.part((cases, e) -> cases.matched = e);

  Production<Node.TagxDeclaration> TagxDeclaration =
      f.production(c -> new Node.TagxDeclaration(), Node.TagxDeclaration::markLast);
  Ephemeral<Node.TagxDeclaration> TagxParameters = f.ephemeral();
  Ephemeral<Node.TagxDeclaration> TagxDefinition = f.ephemeral();
  Ephemeral<Node.TagxDeclaration> TagxElement = f.ephemeral();
  Part<Node.TagxDeclaration, Node.ParameterRecord>
      tagxParameters = f.part((t, r) -> t.parameters = r);
  Part<Node.TagxDeclaration, Node.LiteralMarkup>
      tagxExpression = f.part((t, m) -> t.expression = m);
  Part<Node.TagxDeclaration, Node.StatementsElement>
      tagxStatement = f.part((t, e) -> t.statements.add(e));
  Part<Node.TagxDeclaration, Node.FeatureDeclaration>
      tagxFeature = f.part((t, e) -> t.features.add(e));
  Part<Node.TagxDeclaration, Node.StateField>
      tagxStateField = f.part((t, e) -> t.stateFields.add(e));
  Production<Node.StateField> TagxStateField = f.production(Node.StateField::new);
  Part<Node.StateField, Identifier> stateName = f.part((s, n) -> s.name = n);
  Part<Node.StateField, Node.Expression> stateInitial = f.part((s, e) -> s.initial = e);

  Ephemeral<Void> nlc = f.ephemeral();

  static void define(Grammar g) {
    var nl = any("\n");
    short whitespace = Term.Whitespace;

    Term.Info.registerSymbols(g::term);

    g.ignore(whitespace);

    // -- unit
    g.production(Unit).is(more(element, Element));

    g.production(Element)
        .or(Blanks)
        .or(TypeDeclaration)
        .or(TagxDeclaration)
        .or(ValueBinding)
        .or(FeatureDeclaration)
        .or(Comments);

    // Standalone comments.
    // Comments related to declarations are parsed with declaration itself
    g.production(Comments).is(more(comment, LineComment));

    g.production(LineComment).is("//");

    g.production(Blanks).is(more("\n"));

    g.production(ValueBinding)
        .is(any(comment, LineComment),
            one(name, Name), opt(TypeUse), "=", sure, opt(nlc), one(bindingValue, Expression));

    g.production(TypeDeclaration)
        .is(any(comment, LineComment),
            "type", sure, one(name, Typename),
            opt(shape, ParametersOrConstructors),
            opt(opt(nlc), "impl", sure, one(implFeatures, ImplFeatures)));

    g.production(ImplFeatures)
        .is("{", any(implElement, ImplElement), "}");

    g.production(ImplElement)
        .or(Blanks)
        .or(FeatureDeclaration)
        .or(Comments);

    g.production(ParametersOrConstructors)
        .or(ConstructorCases)
        .or(Parameters);

    g.production(Parameters)
        .or(ParameterProduct)
        .or(ParameterRecord);

    g.production(ConstructorCases)
        .is("{", "|"); // TODO
    // ()
    // (a, b, c T)
    g.production(ParameterProduct)
        .or("(", any(nlc), ")")
        .or("(", nl,
            one(component, ParameterProductComponent),
            any(parameterSeparator, one(component, ParameterProductComponent), sure),
            any(nlc), ")");

    // {x, y, z int, cm String}
    g.production(ParameterRecord)
        .is("{", nl, sure,
            one(field, ParameterNamedGroup),
            any(parameterSeparator, one(field, ParameterNamedGroup)),
            any(nlc), "}");
    // Actually we want to prohibit empty records, but might allow to parse it

    g.ephemeral(parameterSeparator)
        .or(last(","), any("\n"))
        .or(last("//"), any("\n"))
        .or(last("\n"), any("\n"))
        .or(",", any("\n"))
        .or(any("\n"));

    // speculation
    g.ephemeral(beginningOfStatements)
        .or(last("//"))
        .or(last("\n"))
        .or(last("{"));

    g.production(ParameterProductComponent)
        .or(ParameterUnnamed)
        .or(ParameterNamedGroup);

    // String -- just parameter type without name
    g.production(ParameterUnnamed)
        .is(any(comment, LineComment), one(ptype, TypeUse));

    // a, b String
    g.production(ParameterNamedGroup)
        .is(any(comment, LineComment),
            one(pname, Name), sure, any(",", one(pname, Name)),
            one(ptype, TypeUse),
            opt(":", one(pdefault, Expression)),
            opt(comment, LineComment));

    g.production(TypeUse)
        .or(TypeInvariant)
        .or(TypeVariant);

    // a~
    // a?
    g.production(TypeInvariant)
        .or(TypeOptional)
        .or(TypeSlot)
        .or(TypeUnambiguous);

    g.production(TypeUnambiguous)
        .or(TypeArray)
        .or(TypeEmpty)
        //.or(TypeProduct)
        // product type
        // record type
        .or(TypeNominal);

    g.production(TypeEmpty).is("(", ")");

    g.production(TypeOptional)
        .is(one(content, TypeUnambiguous), "?");

    g.production(TypeSlot)
        .is(one(content, TypeUnambiguous), "~");

    g.production(TypeNominal)
        .is(one(tname, Typename), opt(typeArguments, TypeArguments));

    g.production(TypeArguments) // TODO allow some comments and newlines
        .is("<", sure, one(argument, TypeUse), any(",", one(argument, TypeUse)), ">");

    g.production(TypeArray).is("[", one(content, TypeUse), "]");

    g.production(TypeVariant).is("|", "|", "|"); // TODO

    g.production(FeatureDeclaration)
        .is(any(comment, LineComment),
            one(name, Name),
            opt(not(whitespace), one(input, Parameters), "->", sure),
            FeatureDeclarationTrail);

    // May need to clean up this mess and decide on the actual syntax
    g.ephemeral(FeatureDeclarationTrail)
        .or(one(output, TypeUse), sure, opt(statements, Statements))
        .or(last("->"), one(statements, Statements))
        .or("->", one(statements, Statements));

    g.production(Statements)
        .is("{", sure, nl, any(StatementsElement), "}");

    g.ephemeral(StatementsElement)
        .or(nlc)
        .or(one(statement, ReturnStatement))
        .or(one(statement, LocalBinding))
        .or(one(statement, LocalMultiBinding))
        .or(one(statement, StandaloneExpression));

    g.production(ReturnStatement)
        .is("return", sure, opt(returned, Expression));

    g.production(StandaloneExpression)
        .is(beginningOfStatements, one(expression, Expression));

    g.production(LocalBinding)
        .is(beginningOfStatements, one(localName, Name),
            opt(oneOf(slot, "~")), "=", sure, one(localValue, Expression));

    g.production(LocalMultiBinding)
        .is(beginningOfStatements,
            one(localNames, Name), more(",", one(localNames, Name)), "=",
            one(localValues, Expression), any(",", one(localValues, Expression)));

    g.production(Expression)
        .or(IfStatement)
        .or(ForStatement)
        .or(CaseStatement)
        .or(OperatorSoup);

    g.production(OperatorSoup)
        .or(oneOf(prefixOperator,
            Term.Minus,
            Term.Increment,
            Term.Decrement,
            Term.Exclaim), sure, one(operand, FeatureExpression))
        .or(one(operand, FeatureExpression), OperatorBroth);

    g.ephemeral(OperatorBroth)
        .or(oneOf(suffixOperator,
            Term.Increment,
            Term.Decrement))
        .or(opt(any(nlc), oneOf(assignOperator,
                Term.ColonAssign,
                Term.PlusAssign,
                Term.MinusAssign,
                Term.MultiplyAssign,
                Term.DivideAssign,
                Term.ModuloAssign), sure, opt(nlc), one(operand, FeatureExpression)),
            any(any(nlc), oneOf(operator,
                    Term.RangeInclusive,
                    Term.RangeExclusiveBegin,
                    Term.RangeExclusiveEnd,
                    Term.Multiply,
                    Term.Divide,
                    Term.Modulo,
                    Term.Plus,
                    Term.Minus,
                    Term.Greater,
                    Term.Lower,
                    Term.GreaterEquals,
                    Term.LowerEquals,
                    Term.Equals,
                    Term.NotEquals,
                    Term.LogicAnd,
                    Term.LogicOr,
                    Term.OrElse,
                    Term.ArrowLeft), sure, opt(nlc),
                one(operand, FeatureExpression)));

    g.production(FeatureExpression)
        .is(one(base, BaseExpression), any(FeatureSelectOrApply));

    g.ephemeral(FeatureSelectOrApply)
        .or(any(nlc), ".", sure, any(nlc), one(select, Name))
        .or(oneOf(option, "?"))
        .or(not(whitespace), one(apply, FeatureInput));

    g.production(BaseExpression)
        .or(Reference)
        //.or(LoopControl)
        .or(ConstructorApply)
        .or(TypeReference) // should come after ConstructorApply, as starts with Typename too
        .or(LambdaOperatorExpression)
        .or(LambdaExpression)
        .or(LiteralEmpty)
        .or(LiteralProduct)
        .or(LiteralRecord)
        .or(LiteralArray)
        .or(LiteralNumber)
        .or(LiteralBoolean)
        .or(LiteralString)
        .or(LiteralMarkup);

    g.production(ConstructorApply)
        .is(one(constructor, TypeNominal),
            opt(".", one(constructorCase, Typename)),
            not(whitespace), one(constructorInput, FeatureInput));

    g.production(FeatureInput)
        .or(LiteralEmpty)
        .or(LiteralProduct)
        .or(LiteralRecord)
        .or(LiteralArray);

    g.production(LiteralEmpty)
        .is("(", any(nlc), ")");

    g.production(LiteralProduct)
        .is("(", sure, any(nlc), one(componentExpression, Expression),
            any(",", any(nlc), one(componentExpression, Expression)), opt(","), any(nlc), ")");

    g.production(LiteralRecord)
        .or("{", any(nlc), "}") // allow empty literal for now (JS)
        .or("{", any(nlc),
            one(fieldName, Name), ":", sure, any(nlc), one(componentExpression, Expression),
            any(",", any(nlc),
                one(fieldName, Name), ":", any(nlc), one(componentExpression, Expression)),
            opt(","), any(nlc), "}");

    g.production(LiteralArray)
        .or("[", any(nlc), "]")
        .or("[", sure, any(nlc), one(componentExpression, Expression),
            any(",", any(nlc), one(componentExpression, Expression)), any(nlc), "]");

    g.production(LambdaExpression)
        .is(one(lambdaParameter, ParameterProduct), "->", sure, one(lambdaBody,
						ExpressionOrStatements));

    g.production(LambdaOperatorExpression)
        .is(oneOf(lambdaOperator,
            Term.Multiply,
            Term.Divide,
            Term.Modulo,
            Term.Plus,
            Term.Minus,
            Term.Greater,
            Term.Lower,
            Term.GreaterEquals,
            Term.LowerEquals,
            Term.Equals,
            Term.NotEquals));

    g.production(Reference).is(one(referenceName, Name));

    g.production(TypeReference).is(one(referenceName, Typename));

    g.production(LiteralMarkup)
        .or(LiteralTagList)
        .or(LiteralTag);

    g.production(LiteralTagList)
        .is(Term.TagListOpen, sure, any(TagNestedElement), Term.TagListClose);

    g.production(LiteralTag)
        .is(one(tagName, TagName), sure, any(TagAttribute), TagClosing);

    g.production(TagName)
        .is(Term.TagOpen);

    g.ephemeral(TagClosing)
        .or(oneOf(emptyTag, Term.TagOpenEmptyEnd))
        .or(Term.TagOpenEnd, sure, any(TagNestedElement), nl, Term.TagClose);
    // TODO validate that closing tag have the same name - i.e. proper nesting

    g.ephemeral(TagAttribute)
        .is(nl, one(attributeName, TagAttributeName), sure, "=", TagAttributeValue);

    g.production(TagAttributeName).is(Term.TagAttribute);

    g.ephemeral(TagAttributeValue)
        .or(one(attributeValue, LiteralString))
        .or("{", sure, any(nlc), one(attributeValue, Expression), any(nlc), "}");

    g.ephemeral(TagNestedElement)
        .or(one(text, LiteralText))
        .or(one(text, LiteralMarkup))
        .or(more(nlc)) // do not capture these comments, just allow
        .or("{", sure, any(nlc), opt(text, Expression), any(nlc), "}");

    g.production(LiteralString)
        .is(Term.DoubleQuote, any(LiteralStringElement), Term.DoubleQuote);

    g.ephemeral(LiteralStringElement)
        .or(one(text, LiteralText))
        .or("{", sure, any(nlc), one(text, Expression), any(nlc), "}");

    g.production(LiteralText).is(Term.Text);

    g.production(LiteralNumber)
        .is(oneOf(numberKind,
            Term.IntNumber,
            Term.DecNumber,
            Term.ExpNumber,
            Term.BinNumber,
            Term.HexNumber));

    g.production(LiteralBoolean)
        .is(oneOf(booleanValue,
            Term.True,
            Term.False));

    // newline or line comments
    g.ephemeral(nlc).is(oneOf("\n", "//"));

    g.production(Name).is(Term.Name);

    g.production(Typename).is(oneOf(
        Term.Typename,
        Term.Float, Term.Int, Term.Bool,
        Term.i8, Term.u8,
        Term.i16, Term.u16,
        Term.i32, Term.u32,
        Term.i64, Term.u64,
        Term.f32, Term.f64
    ));

    g.production(IfStatement)
        .is("if", sure, one(condition, OperatorSoup),
            opt(nlc),
            one(then, ExpressionOrStatements),
            opt(opt(nlc), "else", sure, opt(nlc),
                one(otherwise, ExpressionOrStatements)));

    g.production(ForStatement)
        .is("for", sure, opt(one(bindFor, Name), any(",", one(bindFor, Name)), ":"),
            one(iterable, OperatorSoup),
            opt(nlc),
            one(body, ExpressionOrStatements));

    g.production(ExpressionOrStatements)
        .or(ReturnStatement)
        .or(Statements)
        .or(Expression);

    g.production(TagxDeclaration)
        .is(any(comment, LineComment),
            "tagx", sure, one(name, Typename),
            opt(TagxParameters), any(nlc), "->", any(nlc), TagxDefinition);

    g.ephemeral(TagxParameters)
        .or("{", any(nlc), "}")
        .or(one(tagxParameters, ParameterRecord));

    g.ephemeral(TagxDefinition)
        .or(one(tagxExpression, LiteralMarkup))
        .or("{", sure, nl, any(TagxElement), any(nlc), "}");

    g.ephemeral(TagxElement)
        .or(nlc)
        .or(one(tagxStateField, TagxStateField))
        .or(one(tagxFeature, FeatureDeclaration))
        .or(one(tagxStatement, ReturnStatement))
        .or(one(tagxStatement, LocalBinding))
        .or(one(tagxStatement, LocalMultiBinding))
        .or(one(tagxStatement, StandaloneExpression));

    g.production(TagxStateField)
        .or(one(stateName, Name), not(whitespace), "~", opt(TypeUse),
            ":", one(stateInitial, Expression));

    g.production(CaseStatement)
        .is("case", sure, one(matched, Expression), "{", more(CaseElements), "}");

    g.ephemeral(CaseElements)
        .is("(", ")", "->", one(outcome, ExpressionOrStatements));
  }

  Ephemeral<Node.CaseStatement> CaseElements = f.ephemeral();
  Part<Node.CaseStatement, Node.ExpressionOrStatements> outcome =
      f.part((s, e) -> s.outcomes.add(e));
  Alternatives<Node.CasePattern> CasePattern = f.alternatives();
  Production<Node.CasePattern> PatternProductNominal =
      f.production(() -> Node.CasePattern.of(Node.CasePattern.Structural.Product));
  Production<Node.CasePattern> PatternProduct =
      f.production(() -> Node.CasePattern.of(Node.CasePattern.Structural.Product));
  Production<Node.CasePattern> PatternRecordNominal =
      f.production(() -> Node.CasePattern.of(Node.CasePattern.Structural.Record));
  Production<Node.CasePattern> PatternRecord =
      f.production(() -> Node.CasePattern.of(Node.CasePattern.Structural.Record));
  Production<Node.CasePattern> PatternArray =
      f.production(() -> Node.CasePattern.of(Node.CasePattern.Structural.Array));
  Production<Node.CasePattern> PatternValue =
      f.production(() -> Node.CasePattern.of(Node.CasePattern.Structural.Value));

  Part<Node.CasePattern, Identifier> bindPattern = f.part((c, b) -> c.bind = b);

  GrammarVm VM = f.complete(Lang::define);
}
