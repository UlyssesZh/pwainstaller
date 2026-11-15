function _createForOfIteratorHelper(r, e) { var t = "undefined" != typeof Symbol && r[Symbol.iterator] || r["@@iterator"]; if (!t) { if (Array.isArray(r) || (t = _unsupportedIterableToArray2(r)) || e && r && "number" == typeof r.length) { t && (r = t); var _n = 0, F = function F() {}; return { s: F, n: function n() { return _n >= r.length ? { done: !0 } : { done: !1, value: r[_n++] }; }, e: function e(r) { throw r; }, f: F }; } throw new TypeError("Invalid attempt to iterate non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); } var o, a, u = !0; return { s: function s() { t = t.call(r); }, n: function n() { u = !0; var r = t.next(); return r.done ? { done: !0 } : { value: r.value, done: u = !1 }; }, e: function e(r) { o = !0, a = r; }, f: function f() { try { u || null == t.return || t.return(); } finally { if (o) throw a; } } }; }
function ownKeys(e, r) { var t = Object.keys(e); if (Object.getOwnPropertySymbols) { var o = Object.getOwnPropertySymbols(e); r && (o = o.filter(function (r) { return Object.getOwnPropertyDescriptor(e, r).enumerable; })), t.push.apply(t, o); } return t; }
function _objectSpread(e) { for (var r = 1; r < arguments.length; r++) { var t = null != arguments[r] ? arguments[r] : {}; r % 2 ? ownKeys(Object(t), !0).forEach(function (r) { _defineProperty2(e, r, t[r]); }) : Object.getOwnPropertyDescriptors ? Object.defineProperties(e, Object.getOwnPropertyDescriptors(t)) : ownKeys(Object(t)).forEach(function (r) { Object.defineProperty(e, r, Object.getOwnPropertyDescriptor(t, r)); }); } return e; }
function _typeof2(o) { "@babel/helpers - typeof"; return _typeof2 = "function" == typeof Symbol && "symbol" == typeof Symbol.iterator ? function (o) { return typeof o; } : function (o) { return o && "function" == typeof Symbol && o.constructor === Symbol && o !== Symbol.prototype ? "symbol" : typeof o; }, _typeof2(o); }
function _inherits(t, e) { if ("function" != typeof e && null !== e) throw new TypeError("Super expression must either be null or a function"); t.prototype = Object.create(e && e.prototype, { constructor: { value: t, writable: !0, configurable: !0 } }), Object.defineProperty(t, "prototype", { writable: !1 }), e && _setPrototypeOf(t, e); }
function _setPrototypeOf(t, e) { return _setPrototypeOf = Object.setPrototypeOf ? Object.setPrototypeOf.bind() : function (t, e) { return t.__proto__ = e, t; }, _setPrototypeOf(t, e); }
function _callSuper(t, o, e) { return o = _getPrototypeOf(o), _possibleConstructorReturn(t, _isNativeReflectConstruct() ? Reflect.construct(o, e || [], _getPrototypeOf(t).constructor) : o.apply(t, e)); }
function _possibleConstructorReturn(t, e) { if (e && ("object" == _typeof2(e) || "function" == typeof e)) return e; if (void 0 !== e) throw new TypeError("Derived constructors may only return object or undefined"); return _assertThisInitialized(t); }
function _assertThisInitialized(e) { if (void 0 === e) throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); return e; }
function _isNativeReflectConstruct() { try { var t = !Boolean.prototype.valueOf.call(Reflect.construct(Boolean, [], function () {})); } catch (t) {} return (_isNativeReflectConstruct = function _isNativeReflectConstruct() { return !!t; })(); }
function _getPrototypeOf(t) { return _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf.bind() : function (t) { return t.__proto__ || Object.getPrototypeOf(t); }, _getPrototypeOf(t); }
function _classCallCheck(a, n) { if (!(a instanceof n)) throw new TypeError("Cannot call a class as a function"); }
function _defineProperties(e, r) { for (var t = 0; t < r.length; t++) { var o = r[t]; o.enumerable = o.enumerable || !1, o.configurable = !0, "value" in o && (o.writable = !0), Object.defineProperty(e, _toPropertyKey(o.key), o); } }
function _createClass(e, r, t) { return r && _defineProperties(e.prototype, r), t && _defineProperties(e, t), Object.defineProperty(e, "prototype", { writable: !1 }), e; }
function _defineProperty2(e, r, t) { return (r = _toPropertyKey(r)) in e ? Object.defineProperty(e, r, { value: t, enumerable: !0, configurable: !0, writable: !0 }) : e[r] = t, e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == _typeof2(i) ? i : i + ""; }
function _toPrimitive(t, e) { if ("object" != _typeof2(t) || !t) return t; var r; if ("undefined" != typeof Symbol && void 0 !== (r = t[Symbol.toPrimitive])) { var i = r.call(t, e || "default"); if ("object" != _typeof2(i)) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === e ? String : Number)(t); }
function _toConsumableArray(r) { return _arrayWithoutHoles(r) || _iterableToArray(r) || _unsupportedIterableToArray2(r) || _nonIterableSpread(); }
function _nonIterableSpread() { throw new TypeError("Invalid attempt to spread non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); }
function _unsupportedIterableToArray2(r, a) { if (r) { if ("string" == typeof r) return _arrayLikeToArray2(r, a); var t = {}.toString.call(r).slice(8, -1); return "Object" === t && r.constructor && (t = r.constructor.name), "Map" === t || "Set" === t ? Array.from(r) : "Arguments" === t || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(t) ? _arrayLikeToArray2(r, a) : void 0; } }
function _iterableToArray(r) { if ("undefined" != typeof Symbol && null != r[Symbol.iterator] || null != r["@@iterator"]) return Array.from(r); }
function _arrayWithoutHoles(r) { if (Array.isArray(r)) return _arrayLikeToArray2(r); }
function _arrayLikeToArray2(r, a) { (null == a || a > r.length) && (a = r.length); for (var e = 0, n = Array(a); e < a; e++) n[e] = r[e]; return n; }
/**
 * @adguard/extended-css - v2.3.0 - Thu Oct 15 2026
 * https://github.com/AdGuardSoftwareLimited/ext-extended-css#readme
 * Copyright (c) 2026 AdGuard. Licensed GPL-3.0
 */
var ExtendedCss = function () {
  'use strict';

  var LEFT_SQUARE_BRACKET = '[';
  var RIGHT_SQUARE_BRACKET = ']';
  var LEFT_PARENTHESIS = '(';
  var RIGHT_PARENTHESIS = ')';
  var LEFT_CURLY_BRACKET = '{';
  var RIGHT_CURLY_BRACKET = '}';
  var BRACKET = {
    SQUARE: {
      LEFT: LEFT_SQUARE_BRACKET,
      RIGHT: RIGHT_SQUARE_BRACKET
    },
    PARENTHESES: {
      LEFT: LEFT_PARENTHESIS,
      RIGHT: RIGHT_PARENTHESIS
    },
    CURLY: {
      LEFT: LEFT_CURLY_BRACKET,
      RIGHT: RIGHT_CURLY_BRACKET
    }
  };
  var SLASH = '/';
  var BACKSLASH = '\\';
  var SPACE = ' ';
  var COMMA = ',';
  var DOT = '.';
  var SEMICOLON = ';';
  var COLON = ':';
  var SINGLE_QUOTE = '\'';
  var DOUBLE_QUOTE = '"';

  // do not consider hyphen `-` as separated mark
  // to avoid pseudo-class names splitting
  // e.g. 'matches-css' or 'if-not'

  var CARET = '^';
  var DOLLAR_SIGN = '$';
  var EQUAL_SIGN = '=';
  var TAB = '\t';
  var CARRIAGE_RETURN = '\r';
  var LINE_FEED = '\n';
  var FORM_FEED = '\f';
  var WHITE_SPACE_CHARACTERS = [SPACE, TAB, CARRIAGE_RETURN, LINE_FEED, FORM_FEED];

  // for universal selector and attributes
  var ASTERISK = '*';
  var ID_MARKER = '#';
  var CLASS_MARKER = DOT;
  var DESCENDANT_COMBINATOR = SPACE;
  var CHILD_COMBINATOR = '>';
  var NEXT_SIBLING_COMBINATOR = '+';
  var SUBSEQUENT_SIBLING_COMBINATOR = '~';
  var COMBINATORS = [DESCENDANT_COMBINATOR, CHILD_COMBINATOR, NEXT_SIBLING_COMBINATOR, SUBSEQUENT_SIBLING_COMBINATOR];
  var SUPPORTED_SELECTOR_MARKS = [LEFT_SQUARE_BRACKET, RIGHT_SQUARE_BRACKET, LEFT_PARENTHESIS, RIGHT_PARENTHESIS, LEFT_CURLY_BRACKET, RIGHT_CURLY_BRACKET, SLASH, BACKSLASH, SEMICOLON, COLON, COMMA, SINGLE_QUOTE, DOUBLE_QUOTE, CARET, DOLLAR_SIGN, ASTERISK, ID_MARKER, CLASS_MARKER, DESCENDANT_COMBINATOR, CHILD_COMBINATOR, NEXT_SIBLING_COMBINATOR, SUBSEQUENT_SIBLING_COMBINATOR, TAB, CARRIAGE_RETURN, LINE_FEED, FORM_FEED];

  /**
   * Limited list of available symbols before slash `/`
   * to check whether it is valid regexp pattern opening.
   */
  var POSSIBLE_MARKS_BEFORE_REGEXP = {
    COMMON: [
    // e.g. ':matches-attr(/data-/)'
    LEFT_PARENTHESIS,
    // e.g. `:matches-attr('/data-/')`
    SINGLE_QUOTE,
    // e.g. ':matches-attr("/data-/")'
    DOUBLE_QUOTE,
    // e.g. ':matches-attr(check=/data-v-/)'
    EQUAL_SIGN,
    // e.g. ':matches-property(inner./_test/=null)'
    DOT,
    // e.g. ':matches-css(height:/20px/)'
    COLON,
    // ':matches-css-after( content  :   /(\\d+\\s)*me/  )'
    SPACE],
    CONTAINS: [
    // e.g. ':contains(/text/)'
    LEFT_PARENTHESIS,
    // e.g. `:contains('/text/')`
    SINGLE_QUOTE,
    // e.g. ':contains("/text/")'
    DOUBLE_QUOTE]
  };
  var SUPPORTED_STYLE_DECLARATION_MARKS = [
  // divider between property and value in declaration
  COLON,
  // divider between declarations
  SEMICOLON,
  // sometimes is needed for value wrapping
  // e.g. 'content: "-"'
  SINGLE_QUOTE, DOUBLE_QUOTE,
  // needed for quote escaping inside the same-type quotes
  BACKSLASH,
  // whitespaces
  SPACE, TAB, CARRIAGE_RETURN, LINE_FEED, FORM_FEED];

  // absolute:
  var CONTAINS_PSEUDO = 'contains';
  var HAS_TEXT_PSEUDO = 'has-text';
  var ABP_CONTAINS_PSEUDO = '-abp-contains';
  var MATCHES_CSS_PSEUDO = 'matches-css';
  var MATCHES_CSS_BEFORE_PSEUDO = 'matches-css-before';
  var MATCHES_CSS_AFTER_PSEUDO = 'matches-css-after';
  var MATCHES_ATTR_PSEUDO_CLASS_MARKER = 'matches-attr';
  var MATCHES_PROPERTY_PSEUDO_CLASS_MARKER = 'matches-property';
  var XPATH_PSEUDO_CLASS_MARKER = 'xpath';
  var NTH_ANCESTOR_PSEUDO_CLASS_MARKER = 'nth-ancestor';
  var CONTAINS_PSEUDO_NAMES = [CONTAINS_PSEUDO, HAS_TEXT_PSEUDO, ABP_CONTAINS_PSEUDO];

  /**
   * Pseudo-class :upward() can get number or selector arg
   * and if the arg is selector it should be standard, not extended
   * so :upward pseudo-class is always absolute.
   */
  var UPWARD_PSEUDO_CLASS_MARKER = 'upward';

  /**
   * Pseudo-class `:remove()` and pseudo-property `remove`
   * are used for element actions, not for element selecting.
   *
   * Selector text should not contain the pseudo-class
   * so selector parser should consider it as invalid
   * and both are handled by stylesheet parser.
   */
  var REMOVE_PSEUDO_MARKER = 'remove';

  /**
   * Pseudo-classes `:remove-attr()` and `:remove-class()` are action operators:
   * they remove matching attributes or classes instead of selecting elements.
   *
   * Selector text should not contain these pseudo-classes,
   * so the selector parser considers them invalid,
   * and they are handled by the stylesheet parser.
   */
  var REMOVE_ATTR_PSEUDO_MARKER = 'remove-attr';
  var REMOVE_CLASS_PSEUDO_MARKER = 'remove-class';

  /**
   * Action pseudo-class markers which are handled by the stylesheet parser
   * and should never reach the selector parser.
   */
  var ACTION_PSEUDO_MARKERS = [REMOVE_PSEUDO_MARKER, REMOVE_ATTR_PSEUDO_MARKER, REMOVE_CLASS_PSEUDO_MARKER];
  /**
   * Action pseudo-class markers which are stored in rule styles
   * and are not CSS properties.
   */
  var ACTION_STYLE_MARKERS = [REMOVE_ATTR_PSEUDO_MARKER, REMOVE_CLASS_PSEUDO_MARKER];
  // relative:
  var HAS_PSEUDO_CLASS_MARKER = 'has';
  var ABP_HAS_PSEUDO_CLASS_MARKER = '-abp-has';
  var HAS_PSEUDO_CLASS_MARKERS = [HAS_PSEUDO_CLASS_MARKER, ABP_HAS_PSEUDO_CLASS_MARKER];
  var IS_PSEUDO_CLASS_MARKER = 'is';
  var NOT_PSEUDO_CLASS_MARKER = 'not';

  // standalone (no-argument pseudo-classes that test the element
  // itself, not a selector-list of related elements):
  var EMPTY_TRIMMED_PSEUDO_CLASS_MARKER = 'empty-trimmed';
  var STANDALONE_PSEUDO_CLASSES = [EMPTY_TRIMMED_PSEUDO_CLASS_MARKER];
  var ABSOLUTE_PSEUDO_CLASSES = [CONTAINS_PSEUDO, HAS_TEXT_PSEUDO, ABP_CONTAINS_PSEUDO, MATCHES_CSS_PSEUDO, MATCHES_CSS_BEFORE_PSEUDO, MATCHES_CSS_AFTER_PSEUDO, MATCHES_ATTR_PSEUDO_CLASS_MARKER, MATCHES_PROPERTY_PSEUDO_CLASS_MARKER, XPATH_PSEUDO_CLASS_MARKER, NTH_ANCESTOR_PSEUDO_CLASS_MARKER, UPWARD_PSEUDO_CLASS_MARKER];
  var RELATIVE_PSEUDO_CLASSES = [].concat(HAS_PSEUDO_CLASS_MARKERS, [IS_PSEUDO_CLASS_MARKER, NOT_PSEUDO_CLASS_MARKER]);
  var SUPPORTED_PSEUDO_CLASSES = [].concat(ABSOLUTE_PSEUDO_CLASSES, _toConsumableArray(RELATIVE_PSEUDO_CLASSES), STANDALONE_PSEUDO_CLASSES);

  // these pseudo-classes should be part of RegularSelector value
  // if its arg does not contain extended selectors.
  // the ast will be checked after the selector is completely parsed
  var OPTIMIZATION_PSEUDO_CLASSES = [NOT_PSEUDO_CLASS_MARKER, IS_PSEUDO_CLASS_MARKER];

  /**
   * ':scope' is used for extended pseudo-class :has(), if-not(), :is() and :not().
   */
  var SCOPE_CSS_PSEUDO_CLASS = ':scope';

  /**
   * ':after' and ':before' are needed for :matches-css() pseudo-class
   * all other are needed for :has() limitation after regular pseudo-elements.
   *
   * @see {@link https://bugs.chromium.org/p/chromium/issues/detail?id=669058#c54} [case 3]
   */
  var REGULAR_PSEUDO_ELEMENTS = {
    AFTER: 'after',
    BACKDROP: 'backdrop',
    BEFORE: 'before',
    CUE: 'cue',
    CUE_REGION: 'cue-region',
    FIRST_LETTER: 'first-letter',
    FIRST_LINE: 'first-line',
    FILE_SELECTION_BUTTON: 'file-selector-button',
    GRAMMAR_ERROR: 'grammar-error',
    MARKER: 'marker',
    PART: 'part',
    PLACEHOLDER: 'placeholder',
    SELECTION: 'selection',
    SLOTTED: 'slotted',
    SPELLING_ERROR: 'spelling-error',
    TARGET_TEXT: 'target-text'
  };

  // ExtendedCss does not support at-rules
  // https://developer.mozilla.org/en-US/docs/Web/CSS/At-rule
  var AT_RULE_MARKER = '@';
  var CONTENT_CSS_PROPERTY = 'content';
  var PSEUDO_PROPERTY_POSITIVE_VALUE = 'true';
  var DEBUG_PSEUDO_PROPERTY_GLOBAL_VALUE = 'global';
  var STANDALONE_ERROR_PREFIX = {
    NO_ARGUMENTS: 'Standalone pseudo-class does not accept arguments'
  };
  var NO_SELECTOR_ERROR_PREFIX = 'Selector should be defined';
  var STYLE_ERROR_PREFIX = {
    NO_STYLE: 'No style declaration found',
    NO_SELECTOR: "".concat(NO_SELECTOR_ERROR_PREFIX, " before style declaration in stylesheet"),
    INVALID_STYLE: 'Invalid style declaration',
    UNCLOSED_STYLE: 'Unclosed style declaration',
    NO_PROPERTY: 'Missing style property in declaration',
    NO_VALUE: 'Missing style value in declaration',
    NO_STYLE_OR_ACTION: 'Style should be declared or action pseudo-class should be used',
    NO_COMMENT: 'Comments are not supported'
  };
  var NO_AT_RULE_ERROR_PREFIX = 'At-rules are not supported';

  /**
   * Error message shape shared by all action pseudo-classes.
   */

  /**
   * Creates error messages for an action pseudo-class.
   *
   * @param marker Action pseudo-class marker, e.g. 'remove-attr'.
   *
   * @returns Error messages of the action pseudo-class.
   */
  var createActionErrorPrefix = function createActionErrorPrefix(marker) {
    return {
      INVALID_ACTION: "Invalid :".concat(marker, "() pseudo-class in selector"),
      NO_TARGET_SELECTOR: "".concat(NO_SELECTOR_ERROR_PREFIX, " before :").concat(marker, "() pseudo-class"),
      MULTIPLE_USAGE: "Pseudo-class :".concat(marker, "() appears more than once in selector"),
      INVALID_POSITION: "Pseudo-class :".concat(marker, "() should be at the end of selector"),
      INVALID_ARG: "Invalid argument of :".concat(marker, "() pseudo-class")
    };
  };
  var REMOVE_ERROR_PREFIX = createActionErrorPrefix(REMOVE_PSEUDO_MARKER);
  var REMOVE_ATTR_ERROR_PREFIX = createActionErrorPrefix(REMOVE_ATTR_PSEUDO_MARKER);
  var REMOVE_CLASS_ERROR_PREFIX = createActionErrorPrefix(REMOVE_CLASS_PSEUDO_MARKER);

  /**
   * Maps an action pseudo-class marker to its error messages.
   */
  var ACTION_ERROR_PREFIXES = _defineProperty2(_defineProperty2(_defineProperty2({}, REMOVE_PSEUDO_MARKER, REMOVE_ERROR_PREFIX), REMOVE_ATTR_PSEUDO_MARKER, REMOVE_ATTR_ERROR_PREFIX), REMOVE_CLASS_PSEUDO_MARKER, REMOVE_CLASS_ERROR_PREFIX);

  /**
   * Quote mark of a quoted string.
   */

  var MATCHING_ELEMENT_ERROR_PREFIX = 'Error while matching element';
  var MAX_STYLE_PROTECTION_COUNT = 50;

  /**
   * Maximum number of times an action (e.g. `:remove-attr()`) is applied
   * to the same element with the same argument.
   * Needed to avoid an endless loop if the page keeps re-adding
   * the removed attribute or class.
   */
  var MAX_ACTION_REMOVALS_COUNT = 50;
  var ID_ATTRIBUTE = 'id';
  var CLASS_ATTRIBUTE = 'class';
  var STYLE_ATTRIBUTE = 'style';

  /**
   * Regexp that matches backward compatible syntaxes.
   */
  var REGEXP_VALID_OLD_SYNTAX = /\[-(?:ext)-([a-z-_]+)=(["'])((?:(?=(\\?))\4.)*?)\2\]/g;

  /**
   * Marker for checking invalid selector after old-syntax normalizing by selector converter.
   */
  var INVALID_OLD_SYNTAX_MARKER = '[-ext-';

  /**
   * Complex replacement function.
   * Undo quote escaping inside of an extended selector.
   *
   * @param match     Whole matched string.
   * @param name      Group 1.
   * @param quoteChar Group 2.
   * @param rawValue  Group 3.
   *
   * @returns Converted string.
   */
  var evaluateMatch = function evaluateMatch(match, name, quoteChar, rawValue) {
    // Unescape quotes
    var re = new RegExp("([^\\\\]|^)\\\\".concat(quoteChar), 'g');
    var value = rawValue.replace(re, "$1".concat(quoteChar));
    return ":".concat(name, "(").concat(value, ")");
  };

  // ':scope' pseudo may be at start of :has() argument
  // but ExtCssDocument.querySelectorAll() already use it for selecting exact element descendants
  var SCOPE_MARKER_REGEXP = /\(:scope >/g;
  var SCOPE_REPLACER = '(>';
  var MATCHES_CSS_PSEUDO_ELEMENT_REGEXP = /(:matches-css)-(before|after)\(/g;
  var convertMatchesCss = function convertMatchesCss(match, extendedPseudoClass, regularPseudoElement) {
    // ':matches-css-before('  -->  ':matches-css(before, '
    // ':matches-css-after('   -->  ':matches-css(after, '
    return "".concat(extendedPseudoClass).concat(BRACKET.PARENTHESES.LEFT).concat(regularPseudoElement).concat(COMMA);
  };

  /**
   * Handles old syntax and :scope inside :has().
   *
   * @param selector Trimmed selector to normalize.
   *
   * @returns Normalized selector.
   * @throws An error on invalid old extended syntax selector.
   */
  var normalize = function normalize(selector) {
    var normalizedSelector = selector.replace(REGEXP_VALID_OLD_SYNTAX, evaluateMatch).replace(SCOPE_MARKER_REGEXP, SCOPE_REPLACER).replace(MATCHES_CSS_PSEUDO_ELEMENT_REGEXP, convertMatchesCss);

    // validate old syntax after normalizing
    // e.g. '[-ext-matches-css-before=\'content:  /^[A-Z][a-z]'
    if (normalizedSelector.includes(INVALID_OLD_SYNTAX_MARKER)) {
      throw new Error("Invalid extended-css old syntax selector: '".concat(selector, "'"));
    }
    return normalizedSelector;
  };

  /**
   * Prepares the rawSelector before tokenization:
   * 1. Trims it.
   * 2. Converts old syntax `[-ext-pseudo-class="..."]` to new one `:pseudo-class(...)`.
   * 3. Handles :scope pseudo inside :has() pseudo-class arg.
   *
   * @param rawSelector Selector with no style declaration.
   * @returns Prepared selector with no style declaration.
   */
  var convert = function convert(rawSelector) {
    var trimmedSelector = rawSelector.trim();
    return normalize(trimmedSelector);
  };

  /**
   * Possible token types.
   *
   * IMPORTANT: it is used as 'const' instead of 'enum' to avoid side effects
   * during ExtendedCss import into other libraries.
   */
  var TOKEN_TYPE = {
    MARK: 'mark',
    WORD: 'word'
  };
  /**
   * Splits `input` string into tokens.
   *
   * @param input Input string to tokenize.
   * @param supportedMarks Array of supported marks to considered as `TOKEN_TYPE.MARK`;
   * all other will be considered as `TOKEN_TYPE.WORD`.
   *
   * @returns Array of tokens.
   */
  var tokenize = function tokenize(input, supportedMarks) {
    // buffer is needed for words collecting while iterating
    var wordBuffer = '';
    // result collection
    var tokens = [];
    var selectorSymbols = input.split('');
    // iterate through selector chars and collect tokens
    selectorSymbols.forEach(function (symbol) {
      if (supportedMarks.includes(symbol)) {
        // if anything was collected to the buffer before
        if (wordBuffer.length > 0) {
          // now it is time to stop buffer collecting and save is as "word"
          tokens.push({
            type: TOKEN_TYPE.WORD,
            value: wordBuffer
          });
          // reset the buffer
          wordBuffer = '';
        }
        // save current symbol as "mark"
        tokens.push({
          type: TOKEN_TYPE.MARK,
          value: symbol
        });
        return;
      }
      // otherwise collect symbol to the buffer
      wordBuffer += symbol;
    });

    // save the last collected word
    if (wordBuffer.length > 0) {
      tokens.push({
        type: TOKEN_TYPE.WORD,
        value: wordBuffer
      });
    }
    return tokens;
  };

  /**
   * Prepares `rawSelector` and splits it into tokens.
   *
   * @param rawSelector Raw css selector.
   *
   * @returns Array of tokens supported for selector.
   */
  var tokenizeSelector = function tokenizeSelector(rawSelector) {
    var selector = convert(rawSelector);
    return tokenize(selector, SUPPORTED_SELECTOR_MARKS);
  };

  /**
   * Splits `attribute` into tokens.
   *
   * @param attribute Input attribute.
   *
   * @returns Array of tokens supported for attribute.
   */
  var tokenizeAttribute = function tokenizeAttribute(attribute) {
    // equal sigh `=` in attribute is considered as `TOKEN_TYPE.MARK`
    return tokenize(attribute, [].concat(SUPPORTED_SELECTOR_MARKS, [EQUAL_SIGN]));
  };
  function _typeof(o) {
    "@babel/helpers - typeof";

    return _typeof = "function" == typeof Symbol && "symbol" == typeof Symbol.iterator ? function (o) {
      return typeof o;
    } : function (o) {
      return o && "function" == typeof Symbol && o.constructor === Symbol && o !== Symbol.prototype ? "symbol" : typeof o;
    }, _typeof(o);
  }
  function toPrimitive(t, r) {
    if ("object" != _typeof(t) || !t) return t;
    var e = t[Symbol.toPrimitive];
    if (void 0 !== e) {
      var i = e.call(t, r || "default");
      if ("object" != _typeof(i)) return i;
      throw new TypeError("@@toPrimitive must return a primitive value.");
    }
    return ("string" === r ? String : Number)(t);
  }
  function toPropertyKey(t) {
    var i = toPrimitive(t, "string");
    return "symbol" == _typeof(i) ? i : i + "";
  }
  function _defineProperty(e, r, t) {
    return (r = toPropertyKey(r)) in e ? Object.defineProperty(e, r, {
      value: t,
      enumerable: !0,
      configurable: !0,
      writable: !0
    }) : e[r] = t, e;
  }

  /**
   * Possible ast node types.
   *
   * IMPORTANT: it is used as 'const' instead of 'enum' to avoid side effects
   * during ExtendedCss import into other libraries.
   */
  var NODE = {
    SELECTOR_LIST: 'SelectorList',
    SELECTOR: 'Selector',
    REGULAR_SELECTOR: 'RegularSelector',
    EXTENDED_SELECTOR: 'ExtendedSelector',
    ABSOLUTE_PSEUDO_CLASS: 'AbsolutePseudoClass',
    RELATIVE_PSEUDO_CLASS: 'RelativePseudoClass'
  };

  /**
   * Universal interface for all node types.
   */

  /**
   * Class needed for creating ast nodes while selector parsing.
   * Used for SelectorList, Selector, ExtendedSelector.
   */
  var AnySelectorNode = /*#__PURE__*/function () {
    /**
     * Creates new ast node.
     *
     * @param type Ast node type.
     */
    function AnySelectorNode(type) {
      _classCallCheck(this, AnySelectorNode);
      _defineProperty(this, "children", []);
      this.type = type;
    }

    /**
     * Adds child node to children array.
     *
     * @param child Ast node.
     */
    return _createClass(AnySelectorNode, [{
      key: "addChild",
      value: function addChild(child) {
        this.children.push(child);
      }
    }]);
  }();
  /**
   * Class needed for creating RegularSelector ast node while selector parsing.
   */
  var RegularSelectorNode = /*#__PURE__*/function (_AnySelectorNode) {
    /**
     * Creates RegularSelector ast node.
     *
     * @param value Value of RegularSelector node.
     */
    function RegularSelectorNode(value) {
      var _this;
      _classCallCheck(this, RegularSelectorNode);
      _this = _callSuper(this, RegularSelectorNode, [NODE.REGULAR_SELECTOR]);
      _this.value = value;
      return _this;
    }
    _inherits(RegularSelectorNode, _AnySelectorNode);
    return _createClass(RegularSelectorNode);
  }(AnySelectorNode);
  /**
   * Class needed for creating RelativePseudoClass ast node while selector parsing.
   */
  var RelativePseudoClassNode = /*#__PURE__*/function (_AnySelectorNode2) {
    /**
     * Creates RegularSelector ast node.
     *
     * @param name Name of RelativePseudoClass node.
     */
    function RelativePseudoClassNode(name) {
      var _this2;
      _classCallCheck(this, RelativePseudoClassNode);
      _this2 = _callSuper(this, RelativePseudoClassNode, [NODE.RELATIVE_PSEUDO_CLASS]);
      _this2.name = name;
      return _this2;
    }
    _inherits(RelativePseudoClassNode, _AnySelectorNode2);
    return _createClass(RelativePseudoClassNode);
  }(AnySelectorNode);
  /**
   * Class needed for creating AbsolutePseudoClass ast node while selector parsing.
   */
  var AbsolutePseudoClassNode = /*#__PURE__*/function (_AnySelectorNode3) {
    /**
     * Creates AbsolutePseudoClass ast node.
     *
     * @param name Name of AbsolutePseudoClass node.
     */
    function AbsolutePseudoClassNode(name) {
      var _this3;
      _classCallCheck(this, AbsolutePseudoClassNode);
      _this3 = _callSuper(this, AbsolutePseudoClassNode, [NODE.ABSOLUTE_PSEUDO_CLASS]);
      _defineProperty(_this3, "value", '');
      _this3.name = name;
      return _this3;
    }
    _inherits(AbsolutePseudoClassNode, _AnySelectorNode3);
    return _createClass(AbsolutePseudoClassNode);
  }(AnySelectorNode);
  /* eslint-disable jsdoc/require-description-complete-sentence */
  /**
   * Root node.
   *
   * SelectorList
   *   : Selector
   *     ...
   *   ;
   */
  /**
   * Selector node.
   *
   * Selector
   *   : RegularSelector
   *   | ExtendedSelector
   *     ...
   *   ;
   */
  /**
   * Regular selector node.
   * It can be selected by querySelectorAll().
   *
   * RegularSelector
   *   : type
   *   : value
   *   ;
   */
  /**
   * Extended selector node.
   *
   * ExtendedSelector
   *   : AbsolutePseudoClass
   *   | RelativePseudoClass
   *   ;
   */
  /**
   * Absolute extended pseudo-class node,
   * i.e. none-selector args.
   *
   * AbsolutePseudoClass
   *   : type
   *   : name
   *   : value
   *   ;
   */
  /**
   * Relative extended pseudo-class node
   * i.e. selector as arg.
   *
   * RelativePseudoClass
   *   : type
   *   : name
   *   : SelectorList
   *   ;
   */
  //
  //  ast example
  //
  //  div.banner > div:has(span, p), a img.ad
  //
  //  SelectorList - div.banner > div:has(span, p), a img.ad
  //      Selector - div.banner > div:has(span, p)
  //          RegularSelector - div.banner > div
  //          ExtendedSelector - :has(span, p)
  //              PseudoClassSelector - :has
  //              SelectorList - span, p
  //                  Selector - span
  //                      RegularSelector - span
  //                  Selector - p
  //                      RegularSelector - p
  //      Selector - a img.ad
  //          RegularSelector - a img.ad
  //
  /**
   * Some browsers do not support Array.prototype.flat()
   * e.g. Opera 42 which is used for browserstack tests.
   *
   * @see {@link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/flat}
   *
   * @param input Array needed to be flatten.
   *
   * @returns Flatten array.
   * @throws An error if array cannot be flatten.
   */
  var flatten = function flatten(input) {
    var stack = [];
    input.forEach(function (el) {
      return stack.push(el);
    });
    var res = [];
    while (stack.length) {
      // pop value from stack
      var next = stack.pop();
      if (!next) {
        throw new Error('Unable to make array flat');
      }
      if (Array.isArray(next)) {
        // push back array items, won't modify the original input
        next.forEach(function (el) {
          return stack.push(el);
        });
      } else {
        res.push(next);
      }
    }
    // reverse to restore input order
    return res.reverse();
  };

  /**
   * Returns first item from `array`.
   *
   * @param array Input array.
   *
   * @returns First array item, or `undefined` if there is no such item.
   */
  var getFirst = function getFirst(array) {
    return array[0];
  };

  /**
   * Returns last item from array.
   *
   * @param array Input array.
   *
   * @returns Last array item, or `undefined` if there is no such item.
   */
  var getLast = function getLast(array) {
    return array[array.length - 1];
  };

  /**
   * Returns array item which is previous to the last one
   * e.g. for `[5, 6, 7, 8]` returns `7`.
   *
   * @param array Input array.
   *
   * @returns Previous to last array item, or `undefined` if there is no such item.
   */
  var getPrevToLast = function getPrevToLast(array) {
    return array[array.length - 2];
  };

  /**
   * Takes array of ast node `children` and returns the child by the `index`.
   *
   * @param array Array of ast node children.
   * @param index Index of needed child in the array.
   * @param errorMessage Optional error message to throw.
   *
   * @returns Array item at `index` position.
   * @throws An error if there is no child with specified `index` in array.
   */
  var getItemByIndex = function getItemByIndex(array, index, errorMessage) {
    var indexChild = array[index];
    if (!indexChild) {
      throw new Error(errorMessage || "No array item found by index ".concat(index));
    }
    return indexChild;
  };
  var NO_REGULAR_SELECTOR_ERROR = 'At least one of Selector node children should be RegularSelector';

  /**
   * Checks whether the type of `astNode` is SelectorList.
   *
   * @param astNode Ast node.
   *
   * @returns True if astNode.type === SelectorList.
   */
  var isSelectorListNode = function isSelectorListNode(astNode) {
    return (astNode === null || astNode === void 0 ? void 0 : astNode.type) === NODE.SELECTOR_LIST;
  };

  /**
   * Checks whether the type of `astNode` is Selector.
   *
   * @param astNode Ast node.
   *
   * @returns True if astNode.type === Selector.
   */
  var isSelectorNode = function isSelectorNode(astNode) {
    return (astNode === null || astNode === void 0 ? void 0 : astNode.type) === NODE.SELECTOR;
  };

  /**
   * Checks whether the type of `astNode` is RegularSelector.
   *
   * @param astNode Ast node.
   *
   * @returns True if astNode.type === RegularSelector.
   */
  var isRegularSelectorNode = function isRegularSelectorNode(astNode) {
    return (astNode === null || astNode === void 0 ? void 0 : astNode.type) === NODE.REGULAR_SELECTOR;
  };

  /**
   * Checks whether the type of `astNode` is ExtendedSelector.
   *
   * @param astNode Ast node.
   *
   * @returns True if astNode.type === ExtendedSelector.
   */
  var isExtendedSelectorNode = function isExtendedSelectorNode(astNode) {
    return astNode.type === NODE.EXTENDED_SELECTOR;
  };

  /**
   * Checks whether the type of `astNode` is AbsolutePseudoClass.
   *
   * @param astNode Ast node.
   *
   * @returns True if astNode.type === AbsolutePseudoClass.
   */
  var isAbsolutePseudoClassNode = function isAbsolutePseudoClassNode(astNode) {
    return (astNode === null || astNode === void 0 ? void 0 : astNode.type) === NODE.ABSOLUTE_PSEUDO_CLASS;
  };

  /**
   * Checks whether the type of `astNode` is RelativePseudoClass.
   *
   * @param astNode Ast node.
   *
   * @returns True if astNode.type === RelativePseudoClass.
   */
  var isRelativePseudoClassNode = function isRelativePseudoClassNode(astNode) {
    return (astNode === null || astNode === void 0 ? void 0 : astNode.type) === NODE.RELATIVE_PSEUDO_CLASS;
  };

  /**
   * Returns name of `astNode`.
   *
   * @param astNode AbsolutePseudoClass or RelativePseudoClass node.
   *
   * @returns Name of `astNode`.
   * @throws An error on unsupported ast node or no name found.
   */
  var getNodeName = function getNodeName(astNode) {
    if (astNode === null) {
      throw new Error('Ast node should be defined');
    }
    if (!isAbsolutePseudoClassNode(astNode) && !isRelativePseudoClassNode(astNode)) {
      throw new Error('Only AbsolutePseudoClass or RelativePseudoClass ast node can have a name');
    }
    if (!astNode.name) {
      throw new Error('Extended pseudo-class should have a name');
    }
    return astNode.name;
  };

  /**
   * Returns value of `astNode`.
   *
   * @param astNode RegularSelector or AbsolutePseudoClass node.
   * @param errorMessage Optional error message if no value found.
   *
   * @returns Value of `astNode`.
   * @throws An error on unsupported ast node or no value found.
   */
  var getNodeValue = function getNodeValue(astNode, errorMessage) {
    if (astNode === null) {
      throw new Error('Ast node should be defined');
    }
    if (!isRegularSelectorNode(astNode) && !isAbsolutePseudoClassNode(astNode)) {
      throw new Error('Only RegularSelector ot AbsolutePseudoClass ast node can have a value');
    }
    if (!astNode.value) {
      throw new Error(errorMessage || 'Ast RegularSelector ot AbsolutePseudoClass node should have a value');
    }
    return astNode.value;
  };

  /**
   * Returns only RegularSelector nodes from `children`.
   *
   * @param children Array of ast node children.
   *
   * @returns Array of RegularSelector nodes.
   */
  var getRegularSelectorNodes = function getRegularSelectorNodes(children) {
    return children.filter(isRegularSelectorNode);
  };

  /**
   * Returns the first RegularSelector node from `children`.
   *
   * @param children Array of ast node children.
   * @param errorMessage Optional error message if no value found.
   *
   * @returns Ast RegularSelector node.
   * @throws An error if no RegularSelector node found.
   */
  var getFirstRegularChild = function getFirstRegularChild(children, errorMessage) {
    var regularSelectorNodes = getRegularSelectorNodes(children);
    var firstRegularSelectorNode = getFirst(regularSelectorNodes);
    if (!firstRegularSelectorNode) {
      throw new Error(errorMessage || NO_REGULAR_SELECTOR_ERROR);
    }
    return firstRegularSelectorNode;
  };

  /**
   * Returns the last RegularSelector node from `children`.
   *
   * @param children Array of ast node children.
   *
   * @returns Ast RegularSelector node.
   * @throws An error if no RegularSelector node found.
   */
  var getLastRegularChild = function getLastRegularChild(children) {
    var regularSelectorNodes = getRegularSelectorNodes(children);
    var lastRegularSelectorNode = getLast(regularSelectorNodes);
    if (!lastRegularSelectorNode) {
      throw new Error(NO_REGULAR_SELECTOR_ERROR);
    }
    return lastRegularSelectorNode;
  };

  /**
   * Returns the only child of `node`.
   *
   * @param node Ast node.
   * @param errorMessage Error message.
   *
   * @returns The only child of ast node.
   * @throws An error if none or more than one child found.
   */
  var getNodeOnlyChild = function getNodeOnlyChild(node, errorMessage) {
    if (node.children.length !== 1) {
      throw new Error(errorMessage);
    }
    var onlyChild = getFirst(node.children);
    if (!onlyChild) {
      throw new Error(errorMessage);
    }
    return onlyChild;
  };

  /**
   * Takes ExtendedSelector node and returns its only child.
   *
   * @param extendedSelectorNode ExtendedSelector ast node.
   *
   * @returns AbsolutePseudoClass or RelativePseudoClass.
   * @throws An error if there is no specific pseudo-class ast node.
   */
  var getPseudoClassNode = function getPseudoClassNode(extendedSelectorNode) {
    return getNodeOnlyChild(extendedSelectorNode, 'Extended selector should be specified');
  };

  /**
   * Takes RelativePseudoClass node and returns its only child
   * which is relative SelectorList node.
   *
   * @param pseudoClassNode RelativePseudoClass.
   *
   * @returns Relative SelectorList node.
   * @throws An error if no selector list found.
   */
  var getRelativeSelectorListNode = function getRelativeSelectorListNode(pseudoClassNode) {
    if (!isRelativePseudoClassNode(pseudoClassNode)) {
      throw new Error('Only RelativePseudoClass node can have relative SelectorList node as child');
    }
    return getNodeOnlyChild(pseudoClassNode, "Missing arg for :".concat(getNodeName(pseudoClassNode), "() pseudo-class"));
  };
  var ATTRIBUTE_CASE_INSENSITIVE_FLAG = 'i';

  /**
   * Checks whether the passed token is supported extended pseudo-class.
   *
   * @param tokenValue Token value to check.
   *
   * @returns True if `tokenValue` is one of supported extended pseudo-class names.
   */
  var isSupportedPseudoClass = function isSupportedPseudoClass(tokenValue) {
    return SUPPORTED_PSEUDO_CLASSES.includes(tokenValue);
  };

  /**
   * Checks whether the passed pseudo-class `name` should be optimized,
   * i.e. :not() and :is().
   *
   * @param name Pseudo-class name.
   *
   * @returns True if `name` is one if pseudo-class which should be optimized.
   */
  var isOptimizationPseudoClass = function isOptimizationPseudoClass(name) {
    return OPTIMIZATION_PSEUDO_CLASSES.includes(name);
  };

  /**
   * Checks whether next to "space" token is a continuation of regular selector being processed.
   *
   * @param nextTokenType Type of token next to current one.
   * @param nextTokenValue Value of token next to current one.
   *
   * @returns True if next token seems to be a part of current regular selector.
   */
  var doesRegularContinueAfterSpace = function doesRegularContinueAfterSpace(nextTokenType, nextTokenValue) {
    // regular selector does not continues after the current token
    if (!nextTokenType || !nextTokenValue) {
      return false;
    }
    return COMBINATORS.includes(nextTokenValue) || nextTokenType === TOKEN_TYPE.WORD
    // e.g. '#main *:has(> .ad)'
    || nextTokenValue === ASTERISK || nextTokenValue === ID_MARKER || nextTokenValue === CLASS_MARKER
    // e.g. 'div :where(.content)'
    || nextTokenValue === COLON
    // e.g. "div[class*=' ']"
    || nextTokenValue === SINGLE_QUOTE
    // e.g. 'div[class*=" "]'
    || nextTokenValue === DOUBLE_QUOTE || nextTokenValue === BRACKET.SQUARE.LEFT;
  };

  /**
   * Checks whether the regexp pattern for pseudo-class arg starts.
   * Needed for `context.isRegexpOpen` flag.
   *
   * @param context Selector parser context.
   * @param prevTokenValue Value of previous token.
   * @param bufferNodeValue Value of bufferNode.
   *
   * @returns True if current token seems to be a start of regexp pseudo-class arg pattern.
   * @throws An error on invalid regexp pattern.
   */
  var isRegexpOpening = function isRegexpOpening(context, prevTokenValue, bufferNodeValue) {
    var lastExtendedPseudoClassName = getLast(context.extendedPseudoNamesStack);
    if (!lastExtendedPseudoClassName) {
      throw new Error('Regexp pattern allowed only in arg of extended pseudo-class');
    }
    // for regexp pattens the slash should not be escaped
    // const isRegexpPatternSlash = prevTokenValue !== BACKSLASH;
    // regexp pattern can be set as arg of pseudo-class
    // which means limited list of available symbols before slash `/`;
    // for :contains() pseudo-class regexp pattern should be at the beginning of arg
    if (CONTAINS_PSEUDO_NAMES.includes(lastExtendedPseudoClassName)) {
      return POSSIBLE_MARKS_BEFORE_REGEXP.CONTAINS.includes(prevTokenValue);
    }
    if (prevTokenValue === SLASH && lastExtendedPseudoClassName !== XPATH_PSEUDO_CLASS_MARKER) {
      var rawArgDesc = bufferNodeValue ? "in arg part: '".concat(bufferNodeValue, "'") : 'arg';
      throw new Error("Invalid regexp pattern for :".concat(lastExtendedPseudoClassName, "() pseudo-class ").concat(rawArgDesc));
    }

    // for other pseudo-classes regexp pattern can be either the whole arg or its part
    return POSSIBLE_MARKS_BEFORE_REGEXP.COMMON.includes(prevTokenValue);
  };

  /**
   * Checks whether the attribute starts.
   *
   * @param tokenValue Value of current token.
   * @param prevTokenValue Previous token value.
   *
   * @returns True if combination of current and previous token seems to be **a start** of attribute.
   */
  var isAttributeOpening = function isAttributeOpening(tokenValue, prevTokenValue) {
    return tokenValue === BRACKET.SQUARE.LEFT && prevTokenValue !== BACKSLASH;
  };

  /**
   * Checks whether the attribute ends.
   *
   * @param context Selector parser context.
   *
   * @returns True if combination of current and previous token seems to be **an end** of attribute.
   * @throws An error on invalid attribute.
   */
  var isAttributeClosing = function isAttributeClosing(context) {
    var _getPrevToLast;
    if (!context.isAttributeBracketsOpen) {
      return false;
    }
    // valid attributes may have extra spaces inside.
    // we get rid of them just to simplify the checking and they are skipped only here:
    //   - spaces will be collected to the ast with spaces as they were declared is selector
    //   - extra spaces in attribute are not relevant to attribute syntax validity
    //     e.g. 'a[ title ]' is the same as 'a[title]'
    //          'div[style *= "MARGIN" i]' is the same as 'div[style*="MARGIN"i]'
    var noSpaceAttr = context.attributeBuffer.split(SPACE).join('');
    // tokenize the prepared attribute string
    var attrTokens = tokenizeAttribute(noSpaceAttr);
    var firstAttrToken = getFirst(attrTokens);
    var firstAttrTokenType = firstAttrToken === null || firstAttrToken === void 0 ? void 0 : firstAttrToken.type;
    var firstAttrTokenValue = firstAttrToken === null || firstAttrToken === void 0 ? void 0 : firstAttrToken.value;
    // signal an error on any mark-type token except backslash
    // e.g. '[="margin"]'
    if (firstAttrTokenType === TOKEN_TYPE.MARK
    // backslash is allowed at start of attribute
    // e.g. '[\\:data-service-slot]'
    && firstAttrTokenValue !== BACKSLASH) {
      // eslint-disable-next-line max-len
      throw new Error("'[".concat(context.attributeBuffer, "]' is not a valid attribute due to '").concat(firstAttrTokenValue, "' at start of it"));
    }
    var lastAttrToken = getLast(attrTokens);
    var lastAttrTokenType = lastAttrToken === null || lastAttrToken === void 0 ? void 0 : lastAttrToken.type;
    var lastAttrTokenValue = lastAttrToken === null || lastAttrToken === void 0 ? void 0 : lastAttrToken.value;
    if (lastAttrTokenValue === EQUAL_SIGN) {
      // e.g. '[style=]'
      throw new Error("'[".concat(context.attributeBuffer, "]' is not a valid attribute due to '").concat(EQUAL_SIGN, "'"));
    }
    var equalSignIndex = attrTokens.findIndex(function (token) {
      return token.type === TOKEN_TYPE.MARK && token.value === EQUAL_SIGN;
    });
    var prevToLastAttrTokenValue = (_getPrevToLast = getPrevToLast(attrTokens)) === null || _getPrevToLast === void 0 ? void 0 : _getPrevToLast.value;
    if (equalSignIndex === -1) {
      // if there is no '=' inside attribute,
      // it must be just attribute name which means the word-type token before closing bracket
      // e.g. 'div[style]'
      if (lastAttrTokenType === TOKEN_TYPE.WORD) {
        return true;
      }
      return prevToLastAttrTokenValue === BACKSLASH
      // some weird attribute are valid too
      // e.g. '[class\\"ads-article\\"]'
      && (lastAttrTokenValue === DOUBLE_QUOTE
      // e.g. "[class\\'ads-article\\']"
      || lastAttrTokenValue === SINGLE_QUOTE);
    }

    // get the value of token next to `=`
    var nextToEqualSignToken = getItemByIndex(attrTokens, equalSignIndex + 1);
    var nextToEqualSignTokenValue = nextToEqualSignToken.value;
    // check whether the attribute value wrapper in quotes
    var isAttrValueQuote = nextToEqualSignTokenValue === SINGLE_QUOTE || nextToEqualSignTokenValue === DOUBLE_QUOTE;

    // for no quotes after `=` the last token before `]` should be a word-type one
    // e.g. 'div[style*=margin]'
    //      'div[style*=MARGIN i]'
    if (!isAttrValueQuote) {
      if (lastAttrTokenType === TOKEN_TYPE.WORD) {
        return true;
      }
      // otherwise signal an error
      // e.g. 'table[style*=border: 0px"]'
      throw new Error("'[".concat(context.attributeBuffer, "]' is not a valid attribute"));
    }

    // otherwise if quotes for value are present
    // the last token before `]` can still be word-type token
    // e.g. 'div[style*="MARGIN" i]'
    if (lastAttrTokenType === TOKEN_TYPE.WORD && (lastAttrTokenValue === null || lastAttrTokenValue === void 0 ? void 0 : lastAttrTokenValue.toLocaleLowerCase()) === ATTRIBUTE_CASE_INSENSITIVE_FLAG) {
      return prevToLastAttrTokenValue === nextToEqualSignTokenValue;
    }

    // eventually if there is quotes for attribute value and last token is not a word,
    // the closing mark should be the same quote as opening one
    return lastAttrTokenValue === nextToEqualSignTokenValue;
  };

  /**
   * Checks whether the character is a whitespace character.
   *
   * @param char Character to check.
   *
   * @returns True if the character is a whitespace character.
   */
  var isWhiteSpaceChar = function isWhiteSpaceChar(char) {
    return char !== undefined && WHITE_SPACE_CHARACTERS.includes(char);
  };

  /**
   * Returns string without suffix.
   *
   * @param str Input string.
   * @param suffix Needed to remove.
   *
   * @returns String without suffix.
   */
  var removeSuffix = function removeSuffix(str, suffix) {
    var index = str.indexOf(suffix, str.length - suffix.length);
    if (index >= 0) {
      return str.substring(0, index);
    }
    return str;
  };

  /**
   * Replaces all `pattern`s with `replacement` in `input` string.
   * String.replaceAll() polyfill because it is not supported by old browsers, e.g. Chrome 55.
   *
   * @see {@link https://caniuse.com/?search=String.replaceAll}
   *
   * @param input Input string to process.
   * @param pattern Find in the input string.
   * @param replacement Replace the pattern with.
   *
   * @returns Modified string.
   */
  var replaceAll = function replaceAll(input, pattern, replacement) {
    if (!input) {
      return input;
    }
    return input.split(pattern).join(replacement);
  };

  /**
   * Converts string pattern to regular expression.
   *
   * @param str String to convert.
   *
   * @returns Regular expression converted from pattern `str`.
   */
  var toRegExp = function toRegExp(str) {
    if (str.startsWith(SLASH) && str.endsWith(SLASH)) {
      return new RegExp(str.slice(1, -1));
    }
    var escaped = str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    return new RegExp(escaped);
  };

  /**
   * Converts any simple type value to string type,
   * e.g. `undefined` -> `'undefined'`.
   *
   * @param value Any type value.
   *
   * @returns String representation of `value`.
   */
  var convertTypeIntoString = function convertTypeIntoString(value) {
    var output;
    switch (value) {
      case undefined:
        output = 'undefined';
        break;
      case null:
        output = 'null';
        break;
      default:
        output = value.toString();
    }
    return output;
  };

  /**
   * Converts instance of string value into other simple types,
   * e.g. `'null'` -> `null`, `'true'` -> `true`.
   *
   * @param value String-type value.
   *
   * @returns Its own type representation of string-type `value`.
   */
  var convertTypeFromString = function convertTypeFromString(value) {
    var numValue = Number(value);
    var output;
    if (!Number.isNaN(numValue)) {
      output = numValue;
    } else {
      switch (value) {
        case 'undefined':
          output = undefined;
          break;
        case 'null':
          output = null;
          break;
        case 'true':
          output = true;
          break;
        case 'false':
          output = false;
          break;
        default:
          output = value;
      }
    }
    return output;
  };

  /**
   * Checks whether the passed `str` is a name of supported absolute extended pseudo-class,
   * e.g. :contains(), :matches-css() etc.
   *
   * @param str Token value to check.
   *
   * @returns True if `str` is one of absolute extended pseudo-class names.
   */
  var isAbsolutePseudoClass = function isAbsolutePseudoClass(str) {
    return ABSOLUTE_PSEUDO_CLASSES.includes(str);
  };

  /**
   * Checks whether the passed `str` is a name of supported relative extended pseudo-class,
   * e.g. :has(), :not() etc.
   *
   * @param str Token value to check.
   *
   * @returns True if `str` is one of relative extended pseudo-class names.
   */
  var isRelativePseudoClass = function isRelativePseudoClass(str) {
    return RELATIVE_PSEUDO_CLASSES.includes(str);
  };

  /**
   * Checks whether the passed `str` is a name of standalone extended pseudo-class,
   * i.e. a no-argument pseudo-class that tests the element itself (e.g. :empty-trimmed).
   *
   * @param str Token value to check.
   *
   * @returns True if `str` is one of standalone pseudo-class names.
   */
  var isStandalonePseudoClass = function isStandalonePseudoClass(str) {
    return STANDALONE_PSEUDO_CLASSES.includes(str);
  };

  /**
   * Regexp literal action argument, e.g. `/^data-/i`.
   * The second capture group contains the regexp flags.
   */
  var REGEXP_ACTION_ARG = /^\/(.+)\/([imu]*)$/;

  /**
   * Plain action argument: no whitespace, parentheses, quotes or commas.
   */
  var PLAIN_ACTION_ARG = /^[^\s()'",]+$/;

  /**
   * Checks whether the string is an action pseudo-class marker.
   *
   * @param marker Marker to check.
   *
   * @returns True if the string is an action pseudo-class marker.
   */
  var isActionPseudoMarker = function isActionPseudoMarker(marker) {
    return ACTION_PSEUDO_MARKERS.includes(marker);
  };

  /**
   * Checks whether the string is an action pseudo-class marker stored in styles.
   *
   * @param marker Marker to check.
   *
   * @returns True if the string is an action style marker.
   */
  var isActionStyleMarker = function isActionStyleMarker(marker) {
    return ACTION_STYLE_MARKERS.includes(marker);
  };

  /**
   * Throws an error for invalid action pseudo-class usage.
   *
   * @param marker Action pseudo-class marker.
   * @param messageKey Key of the error message.
   * @param selector Selector which caused the error.
   *
   * @throws An error with the message for the action pseudo-class.
   */
  var throwActionError = function throwActionError(marker, messageKey, selector) {
    throw new Error("".concat(ACTION_ERROR_PREFIXES[marker][messageKey], ": '").concat(selector, "'"));
  };

  /**
   * Maximum number of cached action argument matchers.
   * The cache is bounded because the `beforeStyleApplied` callback
   * can supply new arguments on every pass.
   */
  var MAX_ACTION_ARG_MATCHERS_COUNT = 100;

  /**
   * Cache of compiled action argument matchers.
   * Entries are evicted in least-recently-used order when the cache is full.
   */
  var actionArgMatchers = new Map();

  /**
   * Checks whether a regexp literal argument compiles.
   *
   * @param arg Regexp literal argument.
   *
   * @returns True if the regexp is valid.
   */
  var isValidRegexpArg = function isValidRegexpArg(arg) {
    var match = arg.match(REGEXP_ACTION_ARG);
    if (!match) {
      return false;
    }
    try {
      new RegExp(match[1], match[2]);
      return true;
    } catch (e) {
      return false;
    }
  };

  /**
   * Normalizes an action argument: strips quotes, validates plain args
   * and compiles regexp literals.
   *
   * @param rawArg Raw action argument.
   *
   * @returns Normalized argument or an empty string if the argument is invalid.
   */
  var normalizeActionArg = function normalizeActionArg(rawArg) {
    var arg = rawArg.trim();
    if (!arg) {
      return '';
    }
    if (arg.startsWith(SLASH)) {
      return isValidRegexpArg(arg) ? arg : '';
    }
    if (arg.length > 1 && (arg.startsWith(SINGLE_QUOTE) && arg.endsWith(SINGLE_QUOTE) || arg.startsWith(DOUBLE_QUOTE) && arg.endsWith(DOUBLE_QUOTE))) {
      var unquoted = arg.slice(1, -1);
      return PLAIN_ACTION_ARG.test(unquoted) ? unquoted : '';
    }
    return PLAIN_ACTION_ARG.test(arg) ? arg : '';
  };

  /**
   * Creates a matcher for an action argument.
   *
   * Plain arguments match exactly; regexp literal arguments match as regexps.
   * Invalid regexp arguments produce a matcher which never matches.
   *
   * @param arg Normalized action argument.
   *
   * @returns Matcher function for attribute or class names.
   */
  var createActionArgMatcher = function createActionArgMatcher(arg) {
    var cached = actionArgMatchers.get(arg);
    if (cached) {
      // refresh the entry to keep the cache least-recently-used
      actionArgMatchers.delete(arg);
      actionArgMatchers.set(arg, cached);
      return cached;
    }
    var matcher;
    var match = arg.match(REGEXP_ACTION_ARG);
    if (match) {
      try {
        var regexp = new RegExp(match[1], match[2]);
        matcher = function matcher(value) {
          return regexp.test(value);
        };
      } catch (e) {
        matcher = function matcher() {
          return false;
        };
      }
    } else {
      matcher = function matcher(value) {
        return value === arg;
      };
    }

    // evict the least recently used entry if the cache is full
    if (actionArgMatchers.size >= MAX_ACTION_ARG_MATCHERS_COUNT) {
      var oldestArg = actionArgMatchers.keys().next().value;
      if (oldestArg !== undefined) {
        actionArgMatchers.delete(oldestArg);
      }
    }
    actionArgMatchers.set(arg, matcher);
    return matcher;
  };

  /**
   * Returns the node which is being collected
   * or null if there is no such one.
   *
   * @param context Selector parser context.
   *
   * @returns Buffer node or null.
   */
  var getBufferNode = function getBufferNode(context) {
    if (context.pathToBufferNode.length === 0) {
      return null;
    }
    // buffer node is always the last in the pathToBufferNode stack
    return getLast(context.pathToBufferNode) || null;
  };

  /**
   * Returns the parent node to the 'buffer node' — which is the one being collected —
   * or null if there is no such one.
   *
   * @param context Selector parser context.
   *
   * @returns Parent node of buffer node or null.
   */
  var getBufferNodeParent = function getBufferNodeParent(context) {
    // at least two nodes should exist — the buffer node and its parent
    // otherwise return null
    if (context.pathToBufferNode.length < 2) {
      return null;
    }
    // since the buffer node is always the last in the pathToBufferNode stack
    // its parent is previous to it in the stack
    return getPrevToLast(context.pathToBufferNode) || null;
  };

  /**
   * Returns last RegularSelector ast node.
   * Needed for parsing of the complex selector with extended pseudo-class inside it.
   *
   * @param context Selector parser context.
   *
   * @returns Ast RegularSelector node.
   * @throws An error if:
   * - bufferNode is absent;
   * - type of bufferNode is unsupported;
   * - no RegularSelector in bufferNode.
   */
  var getContextLastRegularSelectorNode = function getContextLastRegularSelectorNode(context) {
    var bufferNode = getBufferNode(context);
    if (!bufferNode) {
      throw new Error('No bufferNode found');
    }
    if (!isSelectorNode(bufferNode)) {
      throw new Error('Unsupported bufferNode type');
    }
    var lastRegularSelectorNode = getLastRegularChild(bufferNode.children);
    context.pathToBufferNode.push(lastRegularSelectorNode);
    return lastRegularSelectorNode;
  };

  /**
   * Updates needed buffer node value while tokens iterating.
   * For RegularSelector also collects token values to context.attributeBuffer
   * for proper attribute parsing.
   *
   * @param context Selector parser context.
   * @param tokenValue Value of current token.
   *
   * @throws An error if:
   * - no bufferNode;
   * - bufferNode.type is not RegularSelector or AbsolutePseudoClass.
   */
  var updateBufferNode = function updateBufferNode(context, tokenValue) {
    var bufferNode = getBufferNode(context);
    if (bufferNode === null) {
      throw new Error('No bufferNode to update');
    }
    if (isAbsolutePseudoClassNode(bufferNode)) {
      bufferNode.value += tokenValue;
    } else if (isRegularSelectorNode(bufferNode)) {
      bufferNode.value += tokenValue;
      if (context.isAttributeBracketsOpen) {
        context.attributeBuffer += tokenValue;
      }
    } else {
      // eslint-disable-next-line max-len
      throw new Error("".concat(bufferNode.type, " node cannot be updated. Only RegularSelector and AbsolutePseudoClass are supported"));
    }
  };

  /**
   * Adds SelectorList node to context.ast at the start of ast collecting.
   *
   * @param context Selector parser context.
   */
  var addSelectorListNode = function addSelectorListNode(context) {
    var selectorListNode = new AnySelectorNode(NODE.SELECTOR_LIST);
    context.ast = selectorListNode;
    context.pathToBufferNode.push(selectorListNode);
  };

  /**
   * Adds new node to buffer node children.
   * New added node will be considered as buffer node after it.
   *
   * @param context Selector parser context.
   * @param type Type of node to add.
   * @param tokenValue Optional, defaults to `''`, value of processing token.
   *
   * @throws An error if no bufferNode.
   */
  var addAstNodeByType = function addAstNodeByType(context, type) {
    var tokenValue = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : '';
    var bufferNode = getBufferNode(context);
    if (bufferNode === null) {
      throw new Error('No buffer node');
    }
    var node;
    if (type === NODE.REGULAR_SELECTOR) {
      node = new RegularSelectorNode(tokenValue);
    } else if (type === NODE.ABSOLUTE_PSEUDO_CLASS) {
      node = new AbsolutePseudoClassNode(tokenValue);
    } else if (type === NODE.RELATIVE_PSEUDO_CLASS) {
      node = new RelativePseudoClassNode(tokenValue);
    } else {
      // SelectorList || Selector || ExtendedSelector
      node = new AnySelectorNode(type);
    }
    bufferNode.addChild(node);
    context.pathToBufferNode.push(node);
  };

  /**
   * The very beginning of ast collecting.
   *
   * @param context Selector parser context.
   * @param tokenValue Value of regular selector.
   */
  var initAst = function initAst(context, tokenValue) {
    addSelectorListNode(context);
    addAstNodeByType(context, NODE.SELECTOR);
    // RegularSelector node is always the first child of Selector node
    addAstNodeByType(context, NODE.REGULAR_SELECTOR, tokenValue);
  };

  /**
   * Inits selector list subtree for relative extended pseudo-classes, e.g. :has(), :not().
   *
   * @param context Selector parser context.
   * @param tokenValue Optional, defaults to `''`, value of inner regular selector.
   */
  var initRelativeSubtree = function initRelativeSubtree(context) {
    var tokenValue = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : '';
    addAstNodeByType(context, NODE.SELECTOR_LIST);
    addAstNodeByType(context, NODE.SELECTOR);
    addAstNodeByType(context, NODE.REGULAR_SELECTOR, tokenValue);
  };

  /**
   * Goes to closest parent specified by type.
   * Actually updates path to buffer node for proper ast collecting of selectors while parsing.
   *
   * @param context Selector parser context.
   * @param parentType Type of needed parent node in ast.
   */
  var upToClosest = function upToClosest(context, parentType) {
    for (var i = context.pathToBufferNode.length - 1; i >= 0; i -= 1) {
      var _context$pathToBuffer;
      if (((_context$pathToBuffer = context.pathToBufferNode[i]) === null || _context$pathToBuffer === void 0 ? void 0 : _context$pathToBuffer.type) === parentType) {
        context.pathToBufferNode = context.pathToBufferNode.slice(0, i + 1);
        break;
      }
    }
  };

  /**
   * Returns needed buffer node updated due to complex selector parsing.
   *
   * @param context Selector parser context.
   *
   * @returns Ast node for following selector parsing.
   * @throws An error if there is no upper SelectorNode is ast.
   */
  var getUpdatedBufferNode = function getUpdatedBufferNode(context) {
    // it may happen during the parsing of selector list
    // which is an argument of relative pseudo-class
    // e.g. '.banner:has(~span, ~p)'
    // parser position is here  ↑
    // so if after the comma the buffer node type is SelectorList and parent type is RelativePseudoClass
    // we should simply return the current buffer node
    var bufferNode = getBufferNode(context);
    if (bufferNode && isSelectorListNode(bufferNode) && isRelativePseudoClassNode(getBufferNodeParent(context))) {
      return bufferNode;
    }
    upToClosest(context, NODE.SELECTOR);
    var selectorNode = getBufferNode(context);
    if (!selectorNode) {
      throw new Error('No SelectorNode, impossible to continue selector parsing by ExtendedCss');
    }
    var lastSelectorNodeChild = getLast(selectorNode.children);
    var hasExtended = lastSelectorNodeChild && isExtendedSelectorNode(lastSelectorNodeChild)
    // parser position might be inside standard pseudo-class brackets which has space
    // e.g. 'div:contains(/а/):nth-child(100n + 2)'
    && context.standardPseudoBracketsStack.length === 0;
    var supposedPseudoClassNode = hasExtended && getFirst(lastSelectorNodeChild.children);
    var newNeededBufferNode = selectorNode;
    if (supposedPseudoClassNode) {
      // name of pseudo-class for last extended-node child for Selector node
      var lastExtendedPseudoName = hasExtended && supposedPseudoClassNode.name;
      var isLastExtendedNameRelative = lastExtendedPseudoName && isRelativePseudoClass(lastExtendedPseudoName);
      var isLastExtendedNameAbsolute = lastExtendedPseudoName && isAbsolutePseudoClass(lastExtendedPseudoName);
      var hasRelativeExtended = isLastExtendedNameRelative && context.extendedPseudoBracketsStack.length > 0 && context.extendedPseudoBracketsStack.length === context.extendedPseudoNamesStack.length;
      var hasAbsoluteExtended = isLastExtendedNameAbsolute && lastExtendedPseudoName === getLast(context.extendedPseudoNamesStack);
      if (hasRelativeExtended) {
        // return relative selector node to update later
        context.pathToBufferNode.push(lastSelectorNodeChild);
        newNeededBufferNode = supposedPseudoClassNode;
      } else if (hasAbsoluteExtended) {
        // return absolute selector node to update later
        context.pathToBufferNode.push(lastSelectorNodeChild);
        newNeededBufferNode = supposedPseudoClassNode;
      }
    } else if (hasExtended) {
      // return selector node to add new regular selector node later
      newNeededBufferNode = selectorNode;
    } else {
      // otherwise return last regular selector node to update later
      newNeededBufferNode = getContextLastRegularSelectorNode(context);
    }
    // update the path to buffer node properly
    context.pathToBufferNode.push(newNeededBufferNode);
    return newNeededBufferNode;
  };

  /**
   * Checks values of few next tokens on colon token `:` and:
   *  - updates buffer node for following standard pseudo-class;
   *  - adds extended selector ast node for following extended pseudo-class;
   *  - validates some cases of action pseudo-classes and `:has()` usage.
   *
   * @param context Selector parser context.
   * @param selector Selector.
   * @param tokenValue Value of current token.
   * @param nextTokenValue Value of token next to current one.
   * @param nextToNextTokenValue Value of token next to next to current one.
   *
   * @throws An error on action pseudo-class in selector
   * or :has() inside regular pseudo limitation.
   */
  var handleNextTokenOnColon = function handleNextTokenOnColon(context, selector, tokenValue, nextTokenValue, nextToNextTokenValue) {
    if (context.quoteCharacter) {
      // the colon is inside a quoted value and is not a pseudo-class start,
      // e.g. ':upward([data-x=":remove-attr(title)"])'
      updateBufferNode(context, tokenValue);
      return;
    }
    if (!nextTokenValue) {
      throw new Error("Invalid colon ':' at the end of selector: '".concat(selector, "'"));
    }
    if (!isSupportedPseudoClass(nextTokenValue.toLowerCase())) {
      var actionMarker = nextTokenValue.toLowerCase();
      if (isActionPseudoMarker(actionMarker)) {
        // Action pseudo-classes should be handled before
        // as they are not about element selecting but actions with elements
        // e.g. 'body > div:empty:remove()'
        throwActionError(actionMarker, 'INVALID_ACTION', selector);
      }
      // if following token is not an extended pseudo
      // the colon should be collected to value of RegularSelector
      // e.g. '.entry_text:nth-child(2)'
      updateBufferNode(context, tokenValue);
      // check the token after the pseudo and do balance parentheses later
      // only if it is functional pseudo-class (standard with brackets, e.g. ':lang()').
      // no brackets balance needed for such case,
      // parser position is on first colon after the 'div':
      // e.g. 'div:last-child:has(button.privacy-policy__btn)'
      if (nextToNextTokenValue && nextToNextTokenValue === BRACKET.PARENTHESES.LEFT
      // no brackets balance needed for parentheses inside attribute value
      // e.g. 'a[href="javascript:void(0)"]'   <-- parser position is on colon `:`
      // before `void`           ↑
      && !context.isAttributeBracketsOpen) {
        context.standardPseudoNamesStack.push(nextTokenValue);
      }
    } else {
      // it is supported extended pseudo-class.
      // Disallow :has() inside the pseudos accepting only compound selectors
      // https://bugs.chromium.org/p/chromium/issues/detail?id=669058#c54 [2]
      if (HAS_PSEUDO_CLASS_MARKERS.includes(nextTokenValue) && context.standardPseudoNamesStack.length > 0) {
        // eslint-disable-next-line max-len
        throw new Error("Usage of :".concat(nextTokenValue, "() pseudo-class is not allowed inside regular pseudo: '").concat(getLast(context.standardPseudoNamesStack), "'"));
      } else {
        // stop RegularSelector value collecting
        upToClosest(context, NODE.SELECTOR);
        // add ExtendedSelector to Selector children
        addAstNodeByType(context, NODE.EXTENDED_SELECTOR);
      }
    }
  };

  // limit applying of wildcard :is() and :not() pseudo-class only to html children
  // e.g. ':is(.page, .main) > .banner' or '*:not(span):not(p)'
  var IS_OR_NOT_PSEUDO_SELECTING_ROOT = "html ".concat(ASTERISK);

  /**
   * Checks if there are any ExtendedSelector node in selector list.
   *
   * @param selectorList Ast SelectorList node.
   *
   * @returns True if `selectorList` has any inner ExtendedSelector node.
   */
  var hasExtendedSelector = function hasExtendedSelector(selectorList) {
    return selectorList.children.some(function (selectorNode) {
      return selectorNode.children.some(function (selectorNodeChild) {
        return isExtendedSelectorNode(selectorNodeChild);
      });
    });
  };

  /**
   * Converts selector list of RegularSelector nodes to string.
   *
   * @param selectorList Ast SelectorList node.
   *
   * @returns String representation for selector list of regular selectors.
   */
  var selectorListOfRegularsToString = function selectorListOfRegularsToString(selectorList) {
    // if there is no ExtendedSelector in relative SelectorList
    // it means that each Selector node has single child — RegularSelector node
    // and their values should be combined to string
    var standardCssSelectors = selectorList.children.map(function (selectorNode) {
      var selectorOnlyChild = getNodeOnlyChild(selectorNode, 'Ast Selector node should have RegularSelector node');
      return getNodeValue(selectorOnlyChild);
    });
    return standardCssSelectors.join("".concat(COMMA).concat(SPACE));
  };

  /**
   * Updates children of `node` replacing them with `newChildren`.
   * Important: modifies input `node` which is passed by reference.
   *
   * @param node Ast node to update.
   * @param newChildren Array of new children for ast node.
   *
   * @returns Updated ast node.
   */
  var updateNodeChildren = function updateNodeChildren(node, newChildren) {
    node.children = newChildren;
    return node;
  };

  /**
   * Recursively checks whether the ExtendedSelector node should be optimized.
   * It has to be recursive because RelativePseudoClass has inner SelectorList node.
   *
   * @param currExtendedSelectorNode Ast ExtendedSelector node.
   *
   * @returns True is ExtendedSelector should be optimized.
   */
  var _shouldOptimizeExtendedSelector = function shouldOptimizeExtendedSelector(currExtendedSelectorNode) {
    if (currExtendedSelectorNode === null) {
      return false;
    }
    var extendedPseudoClassNode = getPseudoClassNode(currExtendedSelectorNode);
    var pseudoName = getNodeName(extendedPseudoClassNode);
    if (isAbsolutePseudoClass(pseudoName)) {
      return false;
    }
    // Standalone pseudo-classes (e.g. :empty-trimmed) do not have selector-list args.
    // They cannot be optimized and should remain as standalone ExtendedSelector nodes.
    if (isStandalonePseudoClass(pseudoName)) {
      return false;
    }
    var relativeSelectorList = getRelativeSelectorListNode(extendedPseudoClassNode);
    var innerSelectorNodes = relativeSelectorList.children;
    // simple checking for standard selectors in arg of :not() or :is() pseudo-class
    // e.g. 'div > *:is(div, a, span)'
    if (isOptimizationPseudoClass(pseudoName)) {
      var areAllSelectorNodeChildrenRegular = innerSelectorNodes.every(function (selectorNode) {
        try {
          var selectorOnlyChild = getNodeOnlyChild(selectorNode, 'Selector node should have RegularSelector');
          // it means that the only child is RegularSelector and it can be optimized
          return isRegularSelectorNode(selectorOnlyChild);
        } catch (e) {
          return false;
        }
      });
      if (areAllSelectorNodeChildrenRegular) {
        return true;
      }
    }
    // for other extended pseudo-classes than :not() and :is()
    return innerSelectorNodes.some(function (selectorNode) {
      return selectorNode.children.some(function (selectorNodeChild) {
        if (!isExtendedSelectorNode(selectorNodeChild)) {
          return false;
        }
        // check inner ExtendedSelector recursively
        // e.g. 'div:has(*:not(.header))'
        return _shouldOptimizeExtendedSelector(selectorNodeChild);
      });
    });
  };

  /**
   * Returns optimized ExtendedSelector node if it can be optimized
   * or null if ExtendedSelector is fully optimized while function execution
   * which means that value of `prevRegularSelectorNode` is updated.
   *
   * @param currExtendedSelectorNode Current ExtendedSelector node to optimize.
   * @param prevRegularSelectorNode Previous RegularSelector node.
   *
   * @returns Ast node or null.
   */
  var getOptimizedExtendedSelector = function getOptimizedExtendedSelector(currExtendedSelectorNode, prevRegularSelectorNode) {
    if (!currExtendedSelectorNode) {
      return null;
    }
    var extendedPseudoClassNode = getPseudoClassNode(currExtendedSelectorNode);
    var relativeSelectorList = getRelativeSelectorListNode(extendedPseudoClassNode);
    var hasInnerExtendedSelector = hasExtendedSelector(relativeSelectorList);
    if (!hasInnerExtendedSelector) {
      // if there is no extended selectors for :not() or :is()
      // e.g. 'div:not(.content, .main)'
      var relativeSelectorListStr = selectorListOfRegularsToString(relativeSelectorList);
      var pseudoName = getNodeName(extendedPseudoClassNode);
      // eslint-disable-next-line max-len
      var optimizedExtendedStr = "".concat(COLON).concat(pseudoName).concat(BRACKET.PARENTHESES.LEFT).concat(relativeSelectorListStr).concat(BRACKET.PARENTHESES.RIGHT);
      prevRegularSelectorNode.value = "".concat(getNodeValue(prevRegularSelectorNode)).concat(optimizedExtendedStr);
      return null;
    }

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    var optimizedRelativeSelectorList = optimizeSelectorListNode(relativeSelectorList);
    var optimizedExtendedPseudoClassNode = updateNodeChildren(extendedPseudoClassNode, [optimizedRelativeSelectorList]);
    return updateNodeChildren(currExtendedSelectorNode, [optimizedExtendedPseudoClassNode]);
  };

  /**
   * Combines values of `previous` and `current` RegularSelector nodes.
   * It may happen during the optimization when ExtendedSelector between RegularSelector node was optimized.
   *
   * @param current Current RegularSelector node.
   * @param previous Previous RegularSelector node.
   */
  var optimizeCurrentRegularSelector = function optimizeCurrentRegularSelector(current, previous) {
    previous.value = "".concat(getNodeValue(previous)).concat(SPACE).concat(getNodeValue(current));
  };

  /**
   * Optimizes ast Selector node.
   *
   * @param selectorNode Ast Selector node.
   *
   * @returns Optimized ast node.
   * @throws An error while collecting optimized nodes.
   */
  var optimizeSelectorNode = function optimizeSelectorNode(selectorNode) {
    // non-optimized list of SelectorNode children
    var rawSelectorNodeChildren = selectorNode.children;
    // for collecting optimized children list
    var optimizedChildrenList = [];
    var currentIndex = 0;
    // iterate through all children in non-optimized ast Selector node
    while (currentIndex < rawSelectorNodeChildren.length) {
      var currentChild = getItemByIndex(rawSelectorNodeChildren, currentIndex, 'currentChild should be specified');
      // no need to optimize the very first child which is always RegularSelector node
      if (currentIndex === 0) {
        optimizedChildrenList.push(currentChild);
      } else {
        var prevRegularChild = getLastRegularChild(optimizedChildrenList);
        if (isExtendedSelectorNode(currentChild)) {
          // start checking with point is null
          var optimizedExtendedSelector = null;
          // check whether the optimization is needed
          var isOptimizationNeeded = _shouldOptimizeExtendedSelector(currentChild);
          // update optimizedExtendedSelector so it can be optimized recursively
          // i.e. `getOptimizedExtendedSelector(optimizedExtendedSelector)` below
          optimizedExtendedSelector = currentChild;
          while (isOptimizationNeeded) {
            // recursively optimize ExtendedSelector until no optimization needed
            // e.g. div > *:is(.banner:not(.block))
            optimizedExtendedSelector = getOptimizedExtendedSelector(optimizedExtendedSelector, prevRegularChild);
            isOptimizationNeeded = _shouldOptimizeExtendedSelector(optimizedExtendedSelector);
          }
          // if it was simple :not() of :is() with standard selector arg
          // e.g. 'div:not([class][id])'
          // or   '.main > *:is([data-loaded], .banner)'
          // after the optimization the ExtendedSelector node become part of RegularSelector
          // so nothing to save eventually
          // otherwise the optimized ExtendedSelector should be saved
          // e.g. 'div:has(:not([class]))'
          if (optimizedExtendedSelector !== null) {
            optimizedChildrenList.push(optimizedExtendedSelector);
            // if optimization is not needed
            var optimizedPseudoClass = getPseudoClassNode(optimizedExtendedSelector);
            var optimizedPseudoName = getNodeName(optimizedPseudoClass);
            // parent element checking is used to apply :is() and :not() pseudo-classes as extended.
            // as there is no parentNode for root element (html)
            // so element selection should be limited to it's children
            // e.g. '*:is(:has(.page))' -> 'html *:is(has(.page))'
            // or   '*:not(:has(span))' -> 'html *:not(:has(span))'
            if (getNodeValue(prevRegularChild) === ASTERISK && isOptimizationPseudoClass(optimizedPseudoName)) {
              prevRegularChild.value = IS_OR_NOT_PSEUDO_SELECTING_ROOT;
            }
          }
        } else if (isRegularSelectorNode(currentChild)) {
          // in non-optimized ast, RegularSelector node may follow ExtendedSelector which should be optimized
          // for example, for 'div:not(.content) > .banner' schematically it looks like
          // non-optimized ast: [
          //   1. RegularSelector: 'div'
          //   2. ExtendedSelector: 'not(.content)'
          //   3. RegularSelector: '> .banner'
          // ]
          // which after the ExtendedSelector looks like
          // partly optimized ast: [
          //   1. RegularSelector: 'div:not(.content)'
          //   2. RegularSelector: '> .banner'
          // ]
          // so second RegularSelector value should be combined with first one
          // optimized ast: [
          //   1. RegularSelector: 'div:not(.content) > .banner'
          // ]
          // here we check **children of selectorNode** after previous optimization if it was
          var lastOptimizedChild = getLast(optimizedChildrenList) || null;
          if (isRegularSelectorNode(lastOptimizedChild)) {
            optimizeCurrentRegularSelector(currentChild, prevRegularChild);
          }
        }
      }
      currentIndex += 1;
    }
    return updateNodeChildren(selectorNode, optimizedChildrenList);
  };

  /**
   * Optimizes ast SelectorList node.
   *
   * @param selectorListNode SelectorList node.
   *
   * @returns Optimized ast node.
   */
  var optimizeSelectorListNode = function optimizeSelectorListNode(selectorListNode) {
    return updateNodeChildren(selectorListNode, selectorListNode.children.map(function (s) {
      return optimizeSelectorNode(s);
    }));
  };

  /**
   * Optimizes ast:
   * If arg of :not() and :is() pseudo-classes does not contain extended selectors,
   * native Document.querySelectorAll() can be used to query elements.
   * It means that ExtendedSelector ast nodes can be removed
   * and value of relevant RegularSelector node should be updated accordingly.
   *
   * @param ast Non-optimized ast.
   *
   * @returns Optimized ast.
   */
  var optimizeAst = function optimizeAst(ast) {
    // ast is basically the selector list of selectors
    return optimizeSelectorListNode(ast);
  };

  /**
   * State of the selector scanner.
   */

  /**
   * Handlers for structural marks found by the selector scanner.
   */

  /**
   * Creates initial state of the selector scanner.
   *
   * @returns Initial scanner state.
   */
  var createSelectorScannerState = function createSelectorScannerState() {
    return {
      pseudoStack: [],
      quote: undefined,
      inRegexp: false,
      inRegexpCharClass: false,
      prevChar: undefined,
      prevNonWhitespaceChar: undefined,
      pendingPseudo: undefined
    };
  };

  /**
   * Saves a processed character as the previous one.
   *
   * @param state Scanner state.
   * @param char Processed character.
   */
  var setPrevChar = function setPrevChar(state, char) {
    state.prevChar = char;
    if (char && !isWhiteSpaceChar(char)) {
      state.prevNonWhitespaceChar = char;
    }
  };

  /**
   * Checks whether the scanner is inside an `:xpath()` argument.
   *
   * @param state Scanner state.
   *
   * @returns True if the scanner is inside an `:xpath()` argument.
   */
  var isInsideXPathArg = function isInsideXPathArg(state) {
    return state.pseudoStack.includes(XPATH_PSEUDO_CLASS_MARKER);
  };

  /**
   * Checks whether the scanner is inside a `:contains()`-like argument.
   *
   * @param state Scanner state.
   *
   * @returns True if the scanner is inside a `:contains()`-like argument.
   */
  var isInsideContainsArg$1 = function isInsideContainsArg$1(state) {
    return state.pseudoStack.some(function (name) {
      return CONTAINS_PSEUDO_NAMES.includes(name);
    });
  };

  /**
   * Checks whether a quoted string can start at the current scanner position.
   *
   * A quoted string can start only where a string is allowed:
   * - at the beginning of a pseudo-class argument, e.g. `:remove-attr("title")`;
   * - after `=` or `:`, e.g. `[data-x="1"]` or `:matches-css(content: "a)b")`;
   * - after `=` or `:` (possibly followed by whitespace),
   *   e.g. `[data-x = "1"]` or `:upward(div[title = ":remove-attr(x)"])`.
   *
   * Plain text arguments of `:contains()`-like pseudo-classes
   * (e.g. `:contains(the '90s)`) cannot contain quoted strings.
   *
   * @param prevChar Character preceding a quote.
   * @param prevNonWhitespaceChar Last non-whitespace character preceding a quote.
   * @param inContainsArg True if scanning is inside a `:contains()`-like argument.
   *
   * @returns True if a quoted string can start at the current position.
   */
  var canOpenQuote = function canOpenQuote(prevChar, prevNonWhitespaceChar) {
    var inContainsArg = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : false;
    if (inContainsArg) {
      return false;
    }
    if (prevChar === BRACKET.PARENTHESES.LEFT || prevChar === EQUAL_SIGN || prevChar === COLON) {
      return true;
    }
    return isWhiteSpaceChar(prevChar) && (prevNonWhitespaceChar === EQUAL_SIGN || prevNonWhitespaceChar === COLON);
  };

  /**
   * Checks whether a regexp literal can start in the current scanner state.
   *
   * A regexp can start only inside an argument of a pseudo-class which accepts it:
   * at the argument beginning for `:contains()`-like pseudo-classes,
   * after certain marks for other pseudo-classes,
   * and never inside `:xpath()`.
   *
   * @param state Scanner state.
   *
   * @returns True if a regexp literal can start.
   */
  var canOpenRegexp = function canOpenRegexp(state) {
    var pseudoName = state.pseudoStack[state.pseudoStack.length - 1];
    if (!pseudoName) {
      return false;
    }
    if (pseudoName === XPATH_PSEUDO_CLASS_MARKER) {
      return false;
    }
    if (CONTAINS_PSEUDO_NAMES.includes(pseudoName)) {
      return state.prevChar === BRACKET.PARENTHESES.LEFT;
    }
    return POSSIBLE_MARKS_BEFORE_REGEXP.COMMON.includes(state.prevChar) || isWhiteSpaceChar(state.prevChar);
  };

  /**
   * Reads a pseudo-class name after a colon.
   *
   * @param input Scanned string.
   * @param colonIndex Index of the colon.
   *
   * @returns Pseudo-class name or an empty string.
   */
  var readPseudoClassName = function readPseudoClassName(input, colonIndex) {
    var name = '';
    for (var i = colonIndex + 1; i < input.length; i += 1) {
      var char = input[i];
      if (/[a-zA-Z0-9-]/.test(char)) {
        name += char;
      } else {
        break;
      }
    }
    return name;
  };

  /**
   * Processes the character at `index` and updates the scanner state.
   *
   * Escaped characters are consumed as a whole (together with the next character),
   * so the returned index can be greater than `index`.
   * Regexp literals and quoted strings are tracked character by character.
   *
   * @param input Scanned string.
   * @param index Index of the character to process.
   * @param state Scanner state.
   *
   * @returns Index of the last consumed character.
   */
  var updateSelectorScannerState = function updateSelectorScannerState(input, index, state) {
    var char = input[index];
    if (state.inRegexp) {
      if (char === BACKSLASH) {
        setPrevChar(state, char);
        return index + 1;
      }
      if (char === BRACKET.SQUARE.LEFT) {
        state.inRegexpCharClass = true;
      } else if (char === BRACKET.SQUARE.RIGHT) {
        state.inRegexpCharClass = false;
      } else if (char === SLASH && !state.inRegexpCharClass) {
        state.inRegexp = false;
      }
      setPrevChar(state, char);
      return index;
    }
    if (state.quote) {
      if (char === BACKSLASH) {
        setPrevChar(state, char);
        return index + 1;
      }
      if (char === state.quote) {
        state.quote = undefined;
      }
      setPrevChar(state, char);
      return index;
    }
    if (char === BACKSLASH) {
      setPrevChar(state, char);
      return index + 1;
    }
    if (char === SINGLE_QUOTE || char === DOUBLE_QUOTE) {
      // quoted strings can start only where a string is allowed;
      // inside `:xpath()` arguments strings can start anywhere
      if (isInsideXPathArg(state) || canOpenQuote(state.prevChar, state.prevNonWhitespaceChar, isInsideContainsArg$1(state))) {
        state.quote = char;
      }
      setPrevChar(state, char);
      return index;
    }
    if (char === SLASH && canOpenRegexp(state)) {
      state.inRegexp = true;
      setPrevChar(state, char);
      return index;
    }
    if (char === BRACKET.PARENTHESES.LEFT) {
      var _state$pendingPseudo;
      state.pseudoStack.push((_state$pendingPseudo = state.pendingPseudo) !== null && _state$pendingPseudo !== void 0 ? _state$pendingPseudo : '');
      state.pendingPseudo = undefined;
      setPrevChar(state, char);
      return index;
    }
    if (char === BRACKET.PARENTHESES.RIGHT) {
      state.pseudoStack.pop();
      setPrevChar(state, char);
      return index;
    }
    if (char === COLON) {
      var name = readPseudoClassName(input, index);
      if (name && input[index + 1 + name.length] === BRACKET.PARENTHESES.LEFT) {
        state.pendingPseudo = name.toLowerCase();
      }
      setPrevChar(state, char);
      return index;
    }
    setPrevChar(state, char);
    return index;
  };

  /**
   * Scans a selector-like string and calls handlers for its structural marks.
   *
   * @param input Scanned string.
   * @param handlers Handlers for found marks.
   */
  var scanSelector = function scanSelector(input, handlers) {
    var state = createSelectorScannerState();
    for (var i = 0; i < input.length; i += 1) {
      i = updateSelectorScannerState(input, i, state);
      if (state.quote !== undefined || state.inRegexp) {
        // the character is a part of a quoted string or regexp literal
        continue;
      }
      if (input[i] === COLON && state.pseudoStack.length === 0 && handlers.onTopLevelColon) {
        var nextIndex = handlers.onTopLevelColon(i);
        if (typeof nextIndex === 'number') {
          var _input$i;
          // the handler skipped a part of the input, e.g. an action argument
          i = nextIndex;
          state.pendingPseudo = undefined;
          setPrevChar(state, (_input$i = input[i]) !== null && _input$i !== void 0 ? _input$i : '');
        }
      } else if (input[i] === BRACKET.CURLY.LEFT && handlers.onStyleBlockStart) {
        handlers.onStyleBlockStart(i);
        if (handlers.stopAfterFirstStyleBlockStart) {
          break;
        }
      }
    }
  };

  /**
   * Finds the index of the closing parenthesis of an action argument.
   *
   * @param input Scanned string.
   * @param startIndex Index of the first argument character.
   * @param pseudoName Action pseudo-class name.
   *
   * @returns Index of the closing parenthesis or -1 if it is not found.
   */
  var findClosingParenthesis = function findClosingParenthesis(input, startIndex, pseudoName) {
    var state = createSelectorScannerState();
    state.pseudoStack = [pseudoName];
    state.prevChar = BRACKET.PARENTHESES.LEFT;
    for (var i = startIndex; i < input.length; i += 1) {
      i = updateSelectorScannerState(input, i, state);
      if (state.pseudoStack.length === 0) {
        return i;
      }
    }

    // the argument is not closed, e.g. an unclosed regexp literal;
    // `parseActionSelector()` rejects such arguments
    return -1;
  };

  /**
   * Finds the end of an action pseudo-class argument at the given colon.
   *
   * @param input Scanned string.
   * @param colonIndex Index of the action pseudo-class colon.
   *
   * @returns Index of the closing parenthesis or undefined
   * if there is no complete action pseudo-class at the colon.
   */
  var findActionArgumentEnd = function findActionArgumentEnd(input, colonIndex) {
    for (var _i = 0, _ACTION_PSEUDO_MARKER = ACTION_PSEUDO_MARKERS; _i < _ACTION_PSEUDO_MARKER.length; _i++) {
      var marker = _ACTION_PSEUDO_MARKER[_i];
      var markerText = "".concat(marker).concat(BRACKET.PARENTHESES.LEFT);
      var candidate = input.substring(colonIndex + 1, colonIndex + 1 + markerText.length);
      if (candidate.toLowerCase() !== markerText) {
        continue;
      }
      var argStartIndex = colonIndex + 1 + markerText.length;
      var closingIndex = findClosingParenthesis(input, argStartIndex, marker);
      return closingIndex === -1 ? undefined : closingIndex;
    }
    return undefined;
  };

  /**
   * Regexp matching characters which can start a quoted string,
   * a regexp literal or an escape sequence.
   */
  var REGEXP_SPECIAL_CHARS = /['"\\/]/;

  /**
   * Regexp matching an action pseudo-class marker followed by an opening bracket,
   * e.g. `:remove(`, `:remove-attr(` or `:remove-class(`.
   */
  var ACTION_MARKER_REGEXP = new RegExp(":(?:".concat(ACTION_PSEUDO_MARKERS.join('|'), ")\\("), 'i');

  /**
   * Checks whether a prefix cannot contain an open quoted string,
   * regexp literal or action pseudo-class argument:
   * without quotes, slashes and backslashes no string or regexp literal can start,
   * and action pseudo-classes are detected by their markers case-insensitively.
   *
   * @param input Scanned string.
   * @param endIndex Index before which the input should be checked.
   *
   * @returns True if the prefix contains no special characters.
   */
  var isSimplePrefix = function isSimplePrefix(input, endIndex) {
    var prefix = input.slice(0, endIndex);
    return !REGEXP_SPECIAL_CHARS.test(prefix) && !ACTION_MARKER_REGEXP.test(prefix);
  };

  /**
   * Finds indexes of `{` marks which may start a style block.
   *
   * Braces inside quoted strings, regexp literals and action pseudo-class
   * arguments are skipped.
   *
   * @param input Scanned string.
   *
   * @returns Array of left curly bracket indexes.
   */
  var findStyleBlockStarts = function findStyleBlockStarts(input) {
    var indexes = [];
    if (!input.includes(BRACKET.CURLY.LEFT)) {
      return indexes;
    }
    if (isSimplePrefix(input, input.length)) {
      var index = input.indexOf(BRACKET.CURLY.LEFT);
      while (index !== -1) {
        indexes.push(index);
        index = input.indexOf(BRACKET.CURLY.LEFT, index + 1);
      }
      return indexes;
    }
    scanSelector(input, {
      onTopLevelColon: function onTopLevelColon(index) {
        return findActionArgumentEnd(input, index);
      },
      onStyleBlockStart: function onStyleBlockStart(index) {
        indexes.push(index);
      }
    });
    return indexes;
  };

  /**
   * Finds the index of the first `{` mark which may start a style block.
   *
   * Unlike `findStyleBlockStarts()`, the scan stops at the first `{` mark,
   * so the call is linear for the scanned prefix.
   *
   * @param input Scanned string.
   *
   * @returns Index of the first left curly bracket or -1 if it is not found.
   */
  var findStyleBlockStart = function findStyleBlockStart(input) {
    var braceIndex = input.indexOf(BRACKET.CURLY.LEFT);
    // a `{` is required to start a style block
    if (braceIndex === -1) {
      return -1;
    }
    // if no quoted string, regexp literal or action argument can be open
    // before the first `{`, it starts a style block;
    // the check is limited by the first `{` to keep parsing linear
    if (isSimplePrefix(input, braceIndex)) {
      return braceIndex;
    }
    var foundIndex = -1;
    scanSelector(input, {
      onTopLevelColon: function onTopLevelColon(index) {
        return findActionArgumentEnd(input, index);
      },
      onStyleBlockStart: function onStyleBlockStart(index) {
        foundIndex = index;
      },
      stopAfterFirstStyleBlockStart: true
    });
    return foundIndex;
  };

  // limit applying of :xpath() pseudo-class to 'any' element
  // https://github.com/AdguardTeam/ExtendedCss/issues/115
  var XPATH_PSEUDO_SELECTING_ROOT = 'body';
  var NO_WHITESPACE_ERROR_PREFIX = 'No white space is allowed before or after extended pseudo-class name in selector';

  /**
   * Checks whether the parser is inside a `:contains()`-like argument.
   *
   * @param context Parser context.
   *
   * @returns True if the parser is inside a `:contains()`-like argument.
   */
  var isInsideContainsArg = function isInsideContainsArg(context) {
    return context.extendedPseudoNamesStack.some(function (name) {
      return CONTAINS_PSEUDO_NAMES.includes(name);
    });
  };

  /**
   * Normalizes a token value for quote opening checks:
   * `=` is not a separate token, so it can be a part of a word
   * (e.g. `data-x=`), and such a token should be treated as `=`.
   *
   * @param value Token value.
   *
   * @returns Normalized character or the token value as is.
   */
  var normalizeQuoteChar = function normalizeQuoteChar(value) {
    if (value !== null && value !== void 0 && value.endsWith(EQUAL_SIGN)) {
      return EQUAL_SIGN;
    }
    return value;
  };

  /**
   * Checks whether the token is escaped by an odd number of backslashes.
   *
   * @param tokens Selector tokens.
   * @param index Index of the token.
   *
   * @returns True if the token is escaped.
   */
  var isEscapedToken = function isEscapedToken(tokens, index) {
    var backslashCount = 0;
    for (var i = index - 1; i >= 0 && ((_tokens$i = tokens[i]) === null || _tokens$i === void 0 ? void 0 : _tokens$i.value) === BACKSLASH; i -= 1) {
      var _tokens$i;
      backslashCount += 1;
    }
    return backslashCount % 2 === 1;
  };

  /**
   * Parses selector into ast for following element selection.
   *
   * @param selector Selector to parse.
   *
   * @returns Parsed ast.
   * @throws An error on invalid selector.
   */
  var parse = function parse(selector) {
    var tokens = tokenizeSelector(selector);
    var context = {
      ast: null,
      pathToBufferNode: [],
      extendedPseudoNamesStack: [],
      extendedPseudoBracketsStack: [],
      standardPseudoNamesStack: [],
      standardPseudoBracketsStack: [],
      isAttributeBracketsOpen: false,
      attributeBuffer: '',
      isRegexpOpen: false,
      quoteCharacter: undefined,
      shouldOptimize: false
    };

    // the last non-whitespace token value, needed for quote opening checks
    // e.g. '[data-x = ":remove-attr(title)"]'
    var prevNonWhitespaceTokenValue;
    var i = 0;
    while (i < tokens.length) {
      var token = tokens[i];
      if (!token) {
        break;
      }
      // Token to process
      var tokenType = token.type,
        tokenValue = token.value;

      // needed for SPACE and COLON tokens checking
      var nextToken = tokens[i + 1];
      var nextTokenType = nextToken === null || nextToken === void 0 ? void 0 : nextToken.type;
      var nextTokenValue = nextToken === null || nextToken === void 0 ? void 0 : nextToken.value;

      // needed for limitations
      // - :not() and :is() root element
      // - :has() usage
      // - white space before and after pseudo-class name
      var nextToNextToken = tokens[i + 2];
      var nextToNextTokenValue = nextToNextToken === null || nextToNextToken === void 0 ? void 0 : nextToNextToken.value;

      // needed for COLON token checking for none-specified regular selector before extended one
      // e.g. 'p, :hover'
      // or   '.banner, :contains(ads)'
      var previousToken = tokens[i - 1];
      var prevTokenType = previousToken === null || previousToken === void 0 ? void 0 : previousToken.type;
      var prevTokenValue = previousToken === null || previousToken === void 0 ? void 0 : previousToken.value;

      // needed for proper parsing of regexp pattern arg
      // e.g. ':matches-css(background-image: /^url\(https:\/\/example\.org\//)'
      var previousToPreviousToken = tokens[i - 2];
      var prevToPrevTokenValue = previousToPreviousToken === null || previousToPreviousToken === void 0 ? void 0 : previousToPreviousToken.value;

      // track quoted strings to not treat their contents as selector syntax,
      // e.g. ':upward([data-x=":remove-attr(title)"])';
      // quotes in plain text, e.g. ':contains(Don't)', should not open a string
      if ((tokenValue === SINGLE_QUOTE || tokenValue === DOUBLE_QUOTE) && !isEscapedToken(tokens, i) && !context.isRegexpOpen) {
        if (context.quoteCharacter === tokenValue) {
          context.quoteCharacter = undefined;
        } else if (context.quoteCharacter === undefined && canOpenQuote(normalizeQuoteChar(prevTokenValue), normalizeQuoteChar(prevNonWhitespaceTokenValue), isInsideContainsArg(context))) {
          context.quoteCharacter = tokenValue;
        }
      }
      var bufferNode = getBufferNode(context);
      switch (tokenType) {
        case TOKEN_TYPE.WORD:
          if (bufferNode === null) {
            // there is no buffer node only in one case — no ast collecting has been started
            initAst(context, tokenValue);
          } else if (isSelectorListNode(bufferNode)) {
            // add new selector to selector list
            addAstNodeByType(context, NODE.SELECTOR);
            addAstNodeByType(context, NODE.REGULAR_SELECTOR, tokenValue);
          } else if (isRegularSelectorNode(bufferNode)) {
            updateBufferNode(context, tokenValue);
          } else if (isExtendedSelectorNode(bufferNode)) {
            // No white space is allowed between the name of extended pseudo-class
            // and its opening parenthesis
            // https://www.w3.org/TR/selectors-4/#pseudo-classes
            // e.g. 'span:contains (text)'
            if (isWhiteSpaceChar(nextTokenValue) && nextToNextTokenValue === BRACKET.PARENTHESES.LEFT) {
              throw new Error("".concat(NO_WHITESPACE_ERROR_PREFIX, ": '").concat(selector, "'"));
            }
            var lowerCaseTokenValue = tokenValue.toLowerCase();
            var isStandalonePseudo = isStandalonePseudoClass(lowerCaseTokenValue);
            // save pseudo-class name for brackets balance checking only for functional pseudo-classes.
            if (!isStandalonePseudo) {
              context.extendedPseudoNamesStack.push(lowerCaseTokenValue);
            }
            // extended pseudo-class name are parsed in lower case
            // as they should be case-insensitive
            // https://www.w3.org/TR/selectors-4/#pseudo-classes
            if (isAbsolutePseudoClass(lowerCaseTokenValue)) {
              addAstNodeByType(context, NODE.ABSOLUTE_PSEUDO_CLASS, lowerCaseTokenValue);
            } else {
              // if it is not absolute pseudo-class, it must be relative one
              // add RelativePseudoClass with tokenValue as pseudo-class name to ExtendedSelector children
              addAstNodeByType(context, NODE.RELATIVE_PSEUDO_CLASS, lowerCaseTokenValue);
              // for :not() and :is() pseudo-classes parsed ast should be optimized later
              if (isOptimizationPseudoClass(lowerCaseTokenValue)) {
                context.shouldOptimize = true;
              }
              // standalone pseudo-class is complete right after its name.
              if (isStandalonePseudo) {
                // standalone pseudo-classes must not have arguments
                // check if next token is opening parenthesis
                if (nextTokenValue === BRACKET.PARENTHESES.LEFT) {
                  // eslint-disable-next-line max-len
                  throw new Error("".concat(STANDALONE_ERROR_PREFIX.NO_ARGUMENTS, ": ':").concat(lowerCaseTokenValue, "'"));
                }
                upToClosest(context, NODE.SELECTOR);
              }
            }
          } else if (isAbsolutePseudoClassNode(bufferNode)) {
            // collect absolute pseudo-class arg
            updateBufferNode(context, tokenValue);
          } else if (isRelativePseudoClassNode(bufferNode)) {
            initRelativeSubtree(context, tokenValue);
          }
          break;
        case TOKEN_TYPE.MARK:
          switch (tokenValue) {
            case COMMA:
              if (!bufferNode || typeof bufferNode !== 'undefined' && !nextTokenValue) {
                // consider the selector is invalid if there is no bufferNode yet (e.g. ', a')
                // or there is nothing after the comma while bufferNode is defined (e.g. 'div, ')
                throw new Error("'".concat(selector, "' is not a valid selector"));
              } else if (isRegularSelectorNode(bufferNode)) {
                if (context.isAttributeBracketsOpen) {
                  // the comma might be inside element attribute value
                  // e.g. 'div[data-comma="0,1"]'
                  updateBufferNode(context, tokenValue);
                } else {
                  // new Selector should be collected to upper SelectorList
                  upToClosest(context, NODE.SELECTOR_LIST);
                }
              } else if (isAbsolutePseudoClassNode(bufferNode)) {
                // the comma inside arg of absolute extended pseudo
                // e.g. 'div:xpath(//h3[contains(text(),"Share it!")]/..)'
                updateBufferNode(context, tokenValue);
              } else if (isSelectorNode(bufferNode)) {
                // new Selector should be collected to upper SelectorList
                // if parser position is on Selector node
                upToClosest(context, NODE.SELECTOR_LIST);
              }
              break;
            case SPACE:
              // it might be complex selector with extended pseudo-class inside it
              // and the space is between that complex selector and following regular selector
              // parser position is on ` ` before `span` now:
              // e.g. 'div:has(img).banner span'
              // so we need to check whether the new ast node should be added (example above)
              // or previous regular selector node should be updated
              if (isRegularSelectorNode(bufferNode)
              // no need to update the buffer node if attribute value is being parsed
              // e.g. 'div:not([id])[style="position: absolute; z-index: 10000;"]'
              // parser position inside attribute    ↑
              && !context.isAttributeBracketsOpen) {
                bufferNode = getUpdatedBufferNode(context);
              }
              if (isRegularSelectorNode(bufferNode)) {
                // standard selectors with white space between colon and name of pseudo
                // are invalid for native document.querySelectorAll() anyway,
                // so throwing the error here is better
                // than proper parsing of invalid selector and passing it further.
                // first of all do not check attributes
                // e.g. div[style="text-align: center"]
                if (!context.isAttributeBracketsOpen
                // check the space after the colon and before the pseudo
                // e.g. '.block: nth-child(2)
                && (prevTokenValue === COLON && nextTokenType === TOKEN_TYPE.WORD
                // or after the pseudo and before the opening parenthesis
                // e.g. '.block:nth-child (2)
                || prevTokenType === TOKEN_TYPE.WORD && nextTokenValue === BRACKET.PARENTHESES.LEFT)) {
                  throw new Error("'".concat(selector, "' is not a valid selector"));
                }
                // collect current tokenValue to value of RegularSelector
                // if it is the last token or standard selector continues after the space.
                // otherwise it will be skipped
                if (!nextTokenValue || doesRegularContinueAfterSpace(nextTokenType, nextTokenValue)
                // we also should collect space inside attribute value
                // e.g. `[onclick^="window.open ('https://example.com/share?url="]`
                // parser position             ↑
                || context.isAttributeBracketsOpen) {
                  updateBufferNode(context, tokenValue);
                }
              }
              if (isAbsolutePseudoClassNode(bufferNode)) {
                // space inside extended pseudo-class arg
                // e.g. 'span:contains(some text)'
                updateBufferNode(context, tokenValue);
              }
              if (isRelativePseudoClassNode(bufferNode)) {
                // init with empty value RegularSelector
                // as the space is not needed for selector value
                // e.g. 'p:not( .content )'
                initRelativeSubtree(context);
              }
              if (isSelectorNode(bufferNode)) {
                // do NOT add RegularSelector if parser position on space BEFORE the comma in selector list
                // e.g. '.block:has(> img) , .banner)'
                if (doesRegularContinueAfterSpace(nextTokenType, nextTokenValue)) {
                  // regular selector might be after the extended one.
                  // extra space before combinator or selector should not be collected
                  // e.g. '.banner:upward(2) .block'
                  //      '.banner:upward(2) > .block'
                  // so no tokenValue passed to addAnySelectorNode()
                  addAstNodeByType(context, NODE.REGULAR_SELECTOR);
                }
              }
              break;
            case DESCENDANT_COMBINATOR:
            case CHILD_COMBINATOR:
            case NEXT_SIBLING_COMBINATOR:
            case SUBSEQUENT_SIBLING_COMBINATOR:
            case SEMICOLON:
            case SLASH:
            case BACKSLASH:
            case SINGLE_QUOTE:
            case DOUBLE_QUOTE:
            case CARET:
            case DOLLAR_SIGN:
            case BRACKET.CURLY.LEFT:
            case BRACKET.CURLY.RIGHT:
            case ASTERISK:
            case ID_MARKER:
            case CLASS_MARKER:
            case BRACKET.SQUARE.LEFT:
              // it might be complex selector with extended pseudo-class inside it
              // and the space is between that complex selector and following regular selector
              // e.g. 'div:has(img).banner'   // parser position is on `.` before `banner` now
              //      'div:has(img)[attr]'    // parser position is on `[` before `attr` now
              // so we need to check whether the new ast node should be added (example above)
              // or previous regular selector node should be updated
              if (COMBINATORS.includes(tokenValue)) {
                if (bufferNode === null) {
                  // cases where combinator at very beginning of a selector
                  // e.g. '> div'
                  // or   '~ .banner'
                  // or even '+js(overlay-buster)' which not a selector at all
                  // but may be validated by FilterCompiler so error message should be appropriate
                  throw new Error("'".concat(selector, "' is not a valid selector"));
                }
                bufferNode = getUpdatedBufferNode(context);
              }
              if (bufferNode === null) {
                // no ast collecting has been started
                // e.g. '.banner > p'
                // or   '#top > div.ad'
                // or   '[class][style][attr]'
                // or   '*:not(span)'
                initAst(context, tokenValue);
                if (isAttributeOpening(tokenValue, prevTokenValue)) {
                  // e.g. '[class^="banner-"]'
                  context.isAttributeBracketsOpen = true;
                }
              } else if (isRegularSelectorNode(bufferNode)) {
                if (tokenValue === BRACKET.CURLY.LEFT && !(context.isAttributeBracketsOpen || context.isRegexpOpen)) {
                  // e.g. 'div { content: "'
                  throw new Error("'".concat(selector, "' is not a valid selector"));
                }
                // collect the mark to the value of RegularSelector node
                updateBufferNode(context, tokenValue);
                if (isAttributeOpening(tokenValue, prevTokenValue)) {
                  // needed for proper handling element attribute value with comma
                  // e.g. 'div[data-comma="0,1"]'
                  context.isAttributeBracketsOpen = true;
                }
              } else if (isAbsolutePseudoClassNode(bufferNode)) {
                // collect the mark to the arg of AbsolutePseudoClass node
                updateBufferNode(context, tokenValue);
                // 'isRegexpOpen' flag is needed for brackets balancing inside extended pseudo-class arg
                if (tokenValue === SLASH && context.extendedPseudoNamesStack.length > 0) {
                  if (prevTokenValue === SLASH && prevToPrevTokenValue === BACKSLASH) {
                    // it may be specific url regexp pattern in arg of pseudo-class
                    // e.g. ':matches-css(background-image: /^url\(https:\/\/example\.org\//)'
                    // parser position is on final slash before `)`                        ↑
                    context.isRegexpOpen = false;
                  } else if (prevTokenValue && prevTokenValue !== BACKSLASH) {
                    if (isRegexpOpening(context, prevTokenValue, getNodeValue(bufferNode))) {
                      context.isRegexpOpen = !context.isRegexpOpen;
                    } else {
                      // otherwise force `isRegexpOpen` flag to `false`
                      context.isRegexpOpen = false;
                    }
                  }
                }
              } else if (isRelativePseudoClassNode(bufferNode)) {
                // add SelectorList to children of RelativePseudoClass node
                initRelativeSubtree(context, tokenValue);
                if (isAttributeOpening(tokenValue, prevTokenValue)) {
                  // besides of creating the relative subtree
                  // opening square bracket means start of attribute
                  // e.g. 'div:not([class="content"])'
                  //      'div:not([href*="window.print()"])'
                  context.isAttributeBracketsOpen = true;
                }
              } else if (isSelectorNode(bufferNode)) {
                // after the extended pseudo closing parentheses
                // parser position is on Selector node
                // and regular selector can be after the extended one
                // e.g. '.banner:upward(2)> .block'
                // or   '.inner:nth-ancestor(1)~ .banner'
                if (COMBINATORS.includes(tokenValue)) {
                  addAstNodeByType(context, NODE.REGULAR_SELECTOR, tokenValue);
                } else if (!context.isRegexpOpen) {
                  // it might be complex selector with extended pseudo-class inside it.
                  // parser position is on `.` now:
                  // e.g. 'div:has(img).banner'
                  // so we need to get last regular selector node and update its value
                  bufferNode = getContextLastRegularSelectorNode(context);
                  updateBufferNode(context, tokenValue);
                  if (isAttributeOpening(tokenValue, prevTokenValue)) {
                    // handle attribute in compound selector after extended pseudo-class
                    // e.g. 'div:not(.top)[style="z-index: 10000;"]'
                    // parser position    ↑
                    context.isAttributeBracketsOpen = true;
                  }
                }
              } else if (isSelectorListNode(bufferNode)) {
                // add Selector to SelectorList
                addAstNodeByType(context, NODE.SELECTOR);
                // and RegularSelector as it is always the first child of Selector
                addAstNodeByType(context, NODE.REGULAR_SELECTOR, tokenValue);
                if (isAttributeOpening(tokenValue, prevTokenValue)) {
                  // handle simple attribute selector in selector list
                  // e.g. '.banner, [class^="ad-"]'
                  context.isAttributeBracketsOpen = true;
                }
              }
              break;
            case BRACKET.SQUARE.RIGHT:
              if (isRegularSelectorNode(bufferNode)) {
                // unescaped `]` in regular selector allowed only inside attribute value
                if (!context.isAttributeBracketsOpen && prevTokenValue !== BACKSLASH) {
                  // e.g. 'div]'
                  // eslint-disable-next-line max-len
                  throw new Error("'".concat(selector, "' is not a valid selector due to '").concat(tokenValue, "' after '").concat(getNodeValue(bufferNode), "'"));
                }
                // needed for proper parsing regular selectors after the attributes with comma
                // e.g. 'div[data-comma="0,1"] > img'
                if (isAttributeClosing(context)) {
                  context.isAttributeBracketsOpen = false;
                  // reset attribute buffer on closing `]`
                  context.attributeBuffer = '';
                }
                // collect the bracket to the value of RegularSelector node
                updateBufferNode(context, tokenValue);
              }
              if (isAbsolutePseudoClassNode(bufferNode)) {
                // :xpath() expended pseudo-class arg might contain square bracket
                // so it should be collected
                // e.g. 'div:xpath(//h3[contains(text(),"Share it!")]/..)'
                updateBufferNode(context, tokenValue);
              }
              break;
            case COLON:
              // No white space is allowed between the colon and the following name of the pseudo-class
              // https://www.w3.org/TR/selectors-4/#pseudo-classes
              // e.g. 'span: contains(text)'
              if (isWhiteSpaceChar(nextTokenValue) && nextToNextTokenValue && SUPPORTED_PSEUDO_CLASSES.includes(nextToNextTokenValue)) {
                throw new Error("".concat(NO_WHITESPACE_ERROR_PREFIX, ": '").concat(selector, "'"));
              }
              if (bufferNode === null) {
                // no ast collecting has been started
                if (nextTokenValue === XPATH_PSEUDO_CLASS_MARKER) {
                  // limit applying of "naked" :xpath pseudo-class
                  // https://github.com/AdguardTeam/ExtendedCss/issues/115
                  initAst(context, XPATH_PSEUDO_SELECTING_ROOT);
                } else if (nextTokenValue === UPWARD_PSEUDO_CLASS_MARKER || nextTokenValue === NTH_ANCESTOR_PSEUDO_CLASS_MARKER) {
                  // selector should be specified before :nth-ancestor() or :upward()
                  // e.g. ':nth-ancestor(3)'
                  // or   ':upward(span)'
                  throw new Error("".concat(NO_SELECTOR_ERROR_PREFIX, " before :").concat(nextTokenValue, "() pseudo-class"));
                } else {
                  // make it more obvious if selector starts with pseudo with no tag specified
                  // e.g. ':has(a)' -> '*:has(a)'
                  // or   ':empty'  -> '*:empty'
                  initAst(context, ASTERISK);
                }

                // bufferNode should be updated for following checking
                bufferNode = getBufferNode(context);
              }
              if (isSelectorListNode(bufferNode)) {
                // bufferNode is SelectorList after comma has been parsed.
                // parser position is on colon now:
                // e.g. 'img,:not(.content)'
                addAstNodeByType(context, NODE.SELECTOR);
                // add empty value RegularSelector anyway as any selector should start with it
                // and check previous token on the next step
                addAstNodeByType(context, NODE.REGULAR_SELECTOR);
                // bufferNode should be updated for following checking
                bufferNode = getBufferNode(context);
              }
              if (isRegularSelectorNode(bufferNode)) {
                // it can be extended or standard pseudo
                // e.g. '#share, :contains(share it)'
                // or   'div,:hover'
                // of   'div:has(+:contains(text))'  // position is after '+'
                if (prevTokenValue && COMBINATORS.includes(prevTokenValue) || prevTokenValue === COMMA) {
                  // case with colon at the start of string - e.g. ':contains(text)'
                  // is covered by 'bufferNode === null' above at start of COLON checking
                  updateBufferNode(context, ASTERISK);
                }
                handleNextTokenOnColon(context, selector, tokenValue, nextTokenValue, nextToNextTokenValue);
              }
              if (isSelectorNode(bufferNode)) {
                // e.g. 'div:contains(text):'
                if (!nextTokenValue) {
                  throw new Error("Invalid colon ':' at the end of selector: '".concat(selector, "'"));
                }
                // after the extended pseudo closing parentheses
                // parser position is on Selector node
                // and there is might be another extended selector.
                // parser position is on colon before 'upward':
                // e.g. 'p:contains(PR):upward(2)'
                if (isSupportedPseudoClass(nextTokenValue.toLowerCase())) {
                  // if supported extended pseudo-class is next to colon
                  // add ExtendedSelector to Selector children
                  addAstNodeByType(context, NODE.EXTENDED_SELECTOR);
                } else {
                  var actionMarker = nextTokenValue.toLowerCase();
                  if (!context.quoteCharacter && isActionPseudoMarker(actionMarker)) {
                    // Action pseudo-classes should be handled before
                    // as they are not about element selecting but actions with elements
                    // e.g. '#banner:upward(2):remove()'
                    throwActionError(actionMarker, 'INVALID_ACTION', selector);
                  }
                  // otherwise it is standard pseudo after extended pseudo-class in complex selector
                  // and colon should be collected to value of previous RegularSelector
                  // e.g. 'body *:not(input)::selection'
                  //      'input:matches-css(padding: 10):checked'
                  bufferNode = getContextLastRegularSelectorNode(context);
                  handleNextTokenOnColon(context, selector, tokenValue, nextTokenType, nextToNextTokenValue);
                }
              }
              if (isAbsolutePseudoClassNode(bufferNode)) {
                // :xpath() pseudo-class should be the last of extended pseudo-classes
                if (getNodeName(bufferNode) === XPATH_PSEUDO_CLASS_MARKER && nextTokenValue && SUPPORTED_PSEUDO_CLASSES.includes(nextTokenValue) && nextToNextTokenValue === BRACKET.PARENTHESES.LEFT) {
                  throw new Error(":xpath() pseudo-class should be the last in selector: '".concat(selector, "'"));
                }
                // Action pseudo-classes are not allowed inside :upward() arg
                // as it accepts only standard selectors
                // e.g. 'div:upward(:remove-attr(href))'
                var _actionMarker = nextTokenValue === null || nextTokenValue === void 0 ? void 0 : nextTokenValue.toLowerCase();
                if (!context.quoteCharacter && getNodeName(bufferNode) === UPWARD_PSEUDO_CLASS_MARKER && _actionMarker && isActionPseudoMarker(_actionMarker)) {
                  throwActionError(_actionMarker, 'INVALID_ACTION', selector);
                }
                // collecting arg for absolute pseudo-class
                // e.g. 'div:matches-css(width:400px)'
                updateBufferNode(context, tokenValue);
              }
              if (isRelativePseudoClassNode(bufferNode)) {
                if (!nextTokenValue) {
                  // e.g. 'div:has(:'
                  throw new Error("Invalid pseudo-class arg at the end of selector: '".concat(selector, "'"));
                }
                // make it more obvious if selector starts with pseudo with no tag specified
                // parser position is on colon inside :has() arg
                // e.g. 'div:has(:contains(text))'
                // or   'div:not(:empty)'
                initRelativeSubtree(context, ASTERISK);
                if (!isSupportedPseudoClass(nextTokenValue.toLowerCase())) {
                  var _actionMarker2 = nextTokenValue.toLowerCase();
                  if (!context.quoteCharacter && isActionPseudoMarker(_actionMarker2)) {
                    // Action pseudo-classes are not allowed inside relative pseudo-classes
                    // as they are not about element selecting but actions with elements
                    // e.g. 'div:has(:remove-attr(href))'
                    throwActionError(_actionMarker2, 'INVALID_ACTION', selector);
                  }
                  // collect the colon to value of RegularSelector
                  // e.g. 'div:not(:empty)'
                  updateBufferNode(context, tokenValue);
                  // parentheses should be balanced only for functional pseudo-classes
                  // e.g. '.yellow:not(:nth-child(3))'
                  if (nextToNextTokenValue === BRACKET.PARENTHESES.LEFT) {
                    context.standardPseudoNamesStack.push(nextTokenValue);
                  }
                } else {
                  // add ExtendedSelector to Selector children
                  // e.g. 'div:has(:contains(text))'
                  upToClosest(context, NODE.SELECTOR);
                  addAstNodeByType(context, NODE.EXTENDED_SELECTOR);
                }
              }
              break;
            case BRACKET.PARENTHESES.LEFT:
              // start of pseudo-class arg
              if (isAbsolutePseudoClassNode(bufferNode)) {
                // no brackets balancing needed inside
                // 1. :xpath() extended pseudo-class arg
                // 2. regexp arg for other extended pseudo-classes
                if (getNodeName(bufferNode) !== XPATH_PSEUDO_CLASS_MARKER && context.isRegexpOpen) {
                  // if the parentheses is escaped it should be part of regexp
                  // collect it to arg of AbsolutePseudoClass
                  // e.g. 'div:matches-css(background-image: /^url\\("data:image\\/gif;base64.+/)'
                  updateBufferNode(context, tokenValue);
                } else {
                  // otherwise brackets should be balanced
                  // e.g. 'div:xpath(//h3[contains(text(),"Share it!")]/..)'
                  context.extendedPseudoBracketsStack.push(tokenValue);
                  // eslint-disable-next-line max-len
                  if (context.extendedPseudoBracketsStack.length > context.extendedPseudoNamesStack.length) {
                    updateBufferNode(context, tokenValue);
                  }
                }
              }
              if (isRegularSelectorNode(bufferNode)) {
                // continue RegularSelector value collecting for standard pseudo-classes
                // e.g. '.banner:where(div)'
                if (context.standardPseudoNamesStack.length > 0) {
                  updateBufferNode(context, tokenValue);
                  context.standardPseudoBracketsStack.push(tokenValue);
                }
                // parentheses inside attribute value should be part of RegularSelector value
                // e.g. 'div:not([href*="window.print()"])'   <-- parser position
                // is on the `(` after `print`       ↑
                if (context.isAttributeBracketsOpen) {
                  updateBufferNode(context, tokenValue);
                }
              }
              if (isRelativePseudoClassNode(bufferNode)) {
                // save opening bracket for balancing
                // e.g. 'div:not()'  // position is on `(`
                context.extendedPseudoBracketsStack.push(tokenValue);
              }
              break;
            case BRACKET.PARENTHESES.RIGHT:
              if (isAbsolutePseudoClassNode(bufferNode)) {
                // no brackets balancing needed inside
                // 1. :xpath() extended pseudo-class arg
                // 2. regexp arg for other extended pseudo-classes
                if (getNodeName(bufferNode) !== XPATH_PSEUDO_CLASS_MARKER && context.isRegexpOpen) {
                  // if closing bracket is part of regexp
                  // simply save it to pseudo-class arg
                  updateBufferNode(context, tokenValue);
                } else {
                  // remove stacked open parentheses for brackets balance
                  // e.g. 'h3:contains((Ads))'
                  // or   'div:xpath(//h3[contains(text(),"Share it!")]/..)'
                  context.extendedPseudoBracketsStack.pop();
                  if (getNodeName(bufferNode) !== XPATH_PSEUDO_CLASS_MARKER) {
                    // for all other absolute pseudo-classes except :xpath()
                    // remove stacked name of extended pseudo-class
                    context.extendedPseudoNamesStack.pop();
                    // eslint-disable-next-line max-len
                    if (context.extendedPseudoBracketsStack.length > context.extendedPseudoNamesStack.length) {
                      // if brackets stack is not empty yet,
                      // save tokenValue to arg of AbsolutePseudoClass
                      // parser position on first closing bracket after 'Ads':
                      // e.g. 'h3:contains((Ads))'
                      updateBufferNode(context, tokenValue);
                    } else if (context.extendedPseudoBracketsStack.length >= 0 && context.extendedPseudoNamesStack.length >= 0) {
                      // assume it is combined extended pseudo-classes
                      // parser position on first closing bracket after 'advert':
                      // e.g. 'div:has(.banner, :contains(advert))'
                      upToClosest(context, NODE.SELECTOR);
                    }
                  } else {
                    // for :xpath()
                    // eslint-disable-next-line max-len
                    if (context.extendedPseudoBracketsStack.length < context.extendedPseudoNamesStack.length) {
                      // remove stacked name of extended pseudo-class
                      // if there are less brackets than pseudo-class names
                      // with means last removes bracket was closing for pseudo-class
                      context.extendedPseudoNamesStack.pop();
                    } else {
                      // otherwise the bracket is part of arg
                      updateBufferNode(context, tokenValue);
                    }
                  }
                }
              }
              if (isRegularSelectorNode(bufferNode)) {
                if (context.isAttributeBracketsOpen) {
                  // parentheses inside attribute value should be part of RegularSelector value
                  // e.g. 'div:not([href*="window.print()"])'   <-- parser position
                  // is on the `)` after `print(`       ↑
                  updateBufferNode(context, tokenValue);
                } else if (context.standardPseudoNamesStack.length > 0 && context.standardPseudoBracketsStack.length > 0) {
                  // standard pseudo-class was processing.
                  // collect the closing bracket to value of RegularSelector
                  // parser position is on bracket after 'class' now:
                  // e.g. 'div:where(.class)'
                  updateBufferNode(context, tokenValue);
                  // remove bracket and pseudo name from stacks
                  context.standardPseudoBracketsStack.pop();
                  var lastStandardPseudo = context.standardPseudoNamesStack.pop();
                  if (!lastStandardPseudo) {
                    // standard pseudo should be in standardPseudoNamesStack
                    // as related to standardPseudoBracketsStack
                    throw new Error("Parsing error. Invalid selector: ".concat(selector));
                  }
                  // Disallow :has() after regular pseudo-elements
                  // https://bugs.chromium.org/p/chromium/issues/detail?id=669058#c54 [3]
                  if (Object.values(REGULAR_PSEUDO_ELEMENTS).includes(lastStandardPseudo)
                  // check token which is next to closing parentheses and token after it
                  // parser position is on bracket after 'foo' now:
                  // e.g. '::part(foo):has(.a)'
                  && nextTokenValue === COLON && nextToNextTokenValue && HAS_PSEUDO_CLASS_MARKERS.includes(nextToNextTokenValue)) {
                    // eslint-disable-next-line max-len
                    throw new Error("Usage of :".concat(nextToNextTokenValue, "() pseudo-class is not allowed after any regular pseudo-element: '").concat(lastStandardPseudo, "'"));
                  }
                } else {
                  // extended pseudo-class was processing.
                  // e.g. 'div:has(h3)'
                  // remove bracket and pseudo name from stacks
                  context.extendedPseudoBracketsStack.pop();
                  context.extendedPseudoNamesStack.pop();
                  upToClosest(context, NODE.EXTENDED_SELECTOR);
                  // go to upper selector for possible selector continuation after extended pseudo-class
                  // e.g. 'div:has(h3) > img'
                  upToClosest(context, NODE.SELECTOR);
                }
              }
              if (isSelectorNode(bufferNode)) {
                // after inner extended pseudo-class bufferNode is Selector.
                // parser position is on last bracket now:
                // e.g. 'div:has(.banner, :contains(ads))'
                context.extendedPseudoBracketsStack.pop();
                context.extendedPseudoNamesStack.pop();
                upToClosest(context, NODE.EXTENDED_SELECTOR);
                upToClosest(context, NODE.SELECTOR);
              }
              if (isRelativePseudoClassNode(bufferNode)) {
                // save opening bracket for balancing
                // e.g. 'div:not()'  // position is on `)`
                // context.extendedPseudoBracketsStack.push(tokenValue);
                if (context.extendedPseudoNamesStack.length > 0 && context.extendedPseudoBracketsStack.length > 0) {
                  context.extendedPseudoBracketsStack.pop();
                  context.extendedPseudoNamesStack.pop();
                }
              }
              break;
            case LINE_FEED:
            case FORM_FEED:
            case CARRIAGE_RETURN:
              // such characters at start and end of selector should be trimmed
              // so is there is one them among tokens, it is not valid selector
              throw new Error("'".concat(selector, "' is not a valid selector"));
            case TAB:
              // allow tab only inside attribute value
              // as there are such valid rules in filter lists
              // e.g. 'div[style^="margin-right: auto;	text-align: left;',
              // parser position                      ↑
              if (isRegularSelectorNode(bufferNode) && context.isAttributeBracketsOpen) {
                updateBufferNode(context, tokenValue);
              } else {
                // otherwise not valid
                throw new Error("'".concat(selector, "' is not a valid selector"));
              }
          }
          break;
        // no default statement for Marks as they are limited to SUPPORTED_SELECTOR_MARKS
        // and all other symbol combinations are tokenized as Word
        // so error for invalid Word will be thrown later while element selecting by parsed ast
        default:
          throw new Error("Unknown type of token: '".concat(tokenValue, "'"));
      }
      if (!isWhiteSpaceChar(tokenValue)) {
        prevNonWhitespaceTokenValue = tokenValue;
      }
      i += 1;
    }
    if (context.ast === null) {
      throw new Error("'".concat(selector, "' is not a valid selector"));
    }
    if (context.extendedPseudoNamesStack.length > 0 || context.extendedPseudoBracketsStack.length > 0) {
      // eslint-disable-next-line max-len
      throw new Error("Unbalanced brackets for extended pseudo-class: '".concat(getLast(context.extendedPseudoNamesStack), "'"));
    }
    if (context.isAttributeBracketsOpen) {
      throw new Error("Unbalanced attribute brackets in selector: '".concat(selector, "'"));
    }
    return context.shouldOptimize ? optimizeAst(context.ast) : context.ast;
  };
  var natives = {
    MutationObserver: window.MutationObserver || window.WebKitMutationObserver
  };

  /**
   * Class NativeTextContent is needed to intercept and save the native Node textContent getter
   * for proper work of :contains() pseudo-class as it may be mocked.
   *
   * @see {@link https://github.com/AdguardTeam/ExtendedCss/issues/127}
   */
  var NativeTextContent = /*#__PURE__*/function () {
    /**
     * Native Node.
     */

    /**
     * Native Node textContent getter.
     */

    /**
     * Stores native node.
     */
    function NativeTextContent() {
      _classCallCheck(this, NativeTextContent);
      this.nativeNode = window.Node || Node;
    }

    /**
     * Sets native Node textContext getter to `getter` class field.
     */
    return _createClass(NativeTextContent, [{
      key: "setGetter",
      value: function setGetter() {
        var _Object$getOwnPropert;
        this.getter = (_Object$getOwnPropert = Object.getOwnPropertyDescriptor(this.nativeNode.prototype, 'textContent')) === null || _Object$getOwnPropert === void 0 ? void 0 : _Object$getOwnPropert.get;
      }
    }]);
  }();
  var nativeTextContent = new NativeTextContent();

  /**
   * Returns textContent of passed domElement.
   *
   * @param domElement DOM element.
   *
   * @returns DOM element textContent.
   */
  var getNodeTextContent = function getNodeTextContent(domElement) {
    if (nativeTextContent.getter) {
      return nativeTextContent.getter.apply(domElement);
    }
    // if ExtendedCss.init() has not been executed and there is no nodeTextContentGetter,
    // use simple approach, especially when init() is not really needed, e.g. local tests
    return domElement.textContent || '';
  };

  /**
   * Returns element selector text based on it's tagName and attributes.
   *
   * @param element DOM element.
   *
   * @returns String representation of `element`.
   */
  var getElementSelectorDesc = function getElementSelectorDesc(element) {
    var selectorText = element.tagName.toLowerCase();
    selectorText += Array.from(element.attributes).map(function (attr) {
      return "[".concat(attr.name, "=\"").concat(element.getAttribute(attr.name), "\"]");
    }).join('');
    return selectorText;
  };

  /**
   * Returns path to a DOM element as a selector string.
   *
   * @param inputEl Input element.
   *
   * @returns String path to a DOM element.
   * @throws An error if `inputEl` in not instance of `Element`.
   */
  var getElementSelectorPath = function getElementSelectorPath(inputEl) {
    if (!(inputEl instanceof Element)) {
      throw new Error('Function received argument with wrong type');
    }
    var el;
    el = inputEl;
    var path = [];
    // we need to check '!!el' first because it is possible
    // that some ancestor of the inputEl was removed before it
    while (!!el && el.nodeType === Node.ELEMENT_NODE) {
      var selector = el.nodeName.toLowerCase();
      if (el.id && typeof el.id === 'string') {
        selector += "#".concat(el.id);
        path.unshift(selector);
        break;
      }
      var sibling = el;
      var nth = 1;
      while (sibling.previousElementSibling) {
        sibling = sibling.previousElementSibling;
        if (sibling.nodeType === Node.ELEMENT_NODE && sibling.nodeName.toLowerCase() === selector) {
          nth += 1;
        }
      }
      if (nth !== 1) {
        selector += ":nth-of-type(".concat(nth, ")");
      }
      path.unshift(selector);
      el = el.parentElement;
    }
    return path.join(' > ');
  };

  /**
   * Checks whether the element is instance of HTMLElement.
   *
   * @param element Element to check.
   *
   * @returns True if `element` is HTMLElement.
   */
  var isHtmlElement = function isHtmlElement(element) {
    return element instanceof HTMLElement;
  };

  /**
   * Takes `element` and returns its parent element.
   *
   * @param element Element.
   * @param errorMessage Optional error message to throw.
   *
   * @returns Parent of `element`.
   * @throws An error if element has no parent element.
   */
  var getParent = function getParent(element, errorMessage) {
    var parentElement = element.parentElement;
    if (!parentElement) {
      throw new Error(errorMessage || 'Element does no have parent element');
    }
    return parentElement;
  };

  /**
   * Checks whether the `error` has `message` property which type is string.
   *
   * @param error Error object.
   *
   * @returns True if `error` has message.
   */
  var isErrorWithMessage = function isErrorWithMessage(error) {
    return _typeof2(error) === 'object' && error !== null && 'message' in error && typeof error.message === 'string';
  };

  /**
   * Converts `maybeError` to error object with message.
   *
   * @param maybeError Possible error.
   *
   * @returns Error object with defined `message` property.
   */
  var toErrorWithMessage = function toErrorWithMessage(maybeError) {
    if (isErrorWithMessage(maybeError)) {
      return maybeError;
    }
    try {
      return new Error(JSON.stringify(maybeError));
    } catch (_unused) {
      // fallback in case if there is an error happened during the maybeError stringifying
      // like with circular references for example
      return new Error(String(maybeError));
    }
  };

  /**
   * Returns error message from `error`.
   * May be helpful to handle caught errors.
   *
   * @param error Error object.
   *
   * @returns Message of `error`.
   */
  var getErrorMessage = function getErrorMessage(error) {
    return toErrorWithMessage(error).message;
  };
  var logger = {
    /**
     * Safe console.error version.
     */
    error: typeof console !== 'undefined' && console.error && console.error.bind ? console.error.bind(window.console) : console.error,
    /**
     * Safe console.info version.
     */
    info: typeof console !== 'undefined' && console.info && console.info.bind ? console.info.bind(window.console) : console.info
  };
  var SAFARI_USER_AGENT_REGEXP = /\sVersion\/(\d{2}\.\d)(.+\s|\s)(Safari)\//;
  var isSafariBrowser = SAFARI_USER_AGENT_REGEXP.test(navigator.userAgent);

  /**
   * Checks whether the browser userAgent is supported.
   *
   * @param userAgent User agent of browser.
   *
   * @returns False only for Internet Explorer.
   */
  var isUserAgentSupported = function isUserAgentSupported(userAgent) {
    // do not support Internet Explorer
    if (userAgent.includes('MSIE') || userAgent.includes('Trident/')) {
      return false;
    }
    return true;
  };

  /**
   * Checks whether the current browser is supported.
   *
   * @returns False for Internet Explorer, otherwise true.
   */
  var isBrowserSupported = function isBrowserSupported() {
    return isUserAgentSupported(navigator.userAgent);
  };

  /**
   * CSS_PROPERTY is needed for style values normalization.
   *
   * IMPORTANT: it is used as 'const' instead of 'enum' to avoid side effects
   * during ExtendedCss import into other libraries.
   */
  var CSS_PROPERTY = {
    BACKGROUND: 'background',
    BACKGROUND_IMAGE: 'background-image',
    CONTENT: 'content',
    OPACITY: 'opacity'
  };
  var REGEXP_ANY_SYMBOL = '.*';
  var REGEXP_WITH_FLAGS_REGEXP = /^\s*\/.*\/[gmisuy]*\s*$/;
  /**
   * Removes quotes for specified content value.
   *
   * For example, content style declaration with `::before` can be set as '-' (e.g. unordered list)
   * which displayed as simple dash `-` with no quotes.
   * But CSSStyleDeclaration.getPropertyValue('content') will return value
   * wrapped into quotes, e.g. '"-"', which should be removed
   * because filters maintainers does not use any quotes in real rules.
   *
   * @param str Input string.
   *
   * @returns String with no quotes for content value.
   */
  var removeContentQuotes = function removeContentQuotes(str) {
    return str.replace(/^(["'])([\s\S]*)\1$/, '$2');
  };

  /**
   * Adds quotes for specified background url value.
   *
   * If background-image is specified **without** quotes:
   * e.g. 'background: url(data:image/gif;base64,R0lGODlhAQA7)'.
   *
   * CSSStyleDeclaration.getPropertyValue('background-image') may return value **with** quotes:
   * e.g. 'background: url("data:image/gif;base64,R0lGODlhAQA7")'.
   *
   * So we add quotes for compatibility since filters maintainers might use quotes in real rules.
   *
   * @param str Input string.
   *
   * @returns String with unified quotes for background url value.
   */
  var addUrlPropertyQuotes = function addUrlPropertyQuotes(str) {
    if (!str.includes('url("')) {
      var re = /url\((.*?)\)/g;
      return str.replace(re, 'url("$1")');
    }
    return str;
  };

  /**
   * Adds quotes to url arg for consistent property value matching.
   */
  var addUrlQuotesTo = {
    regexpArg: function regexpArg(str) {
      // e.g. /^url\\([a-z]{4}:[a-z]{5}/
      // or /^url\\(data\\:\\image\\/gif;base64.+/
      var re = /(\^)?url(\\)?\\\((\w|\[\w)/g;
      return str.replace(re, '$1url$2\\(\\"?$3');
    },
    noneRegexpArg: addUrlPropertyQuotes
  };

  /**
   * Escapes regular expression string.
   *
   * @see {@link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/regexp}
   *
   * @param str Input string.
   *
   * @returns Escaped regular expression string.
   */
  var escapeRegExp = function escapeRegExp(str) {
    // should be escaped . * + ? ^ $ { } ( ) | [ ] / \
    // except of * | ^
    var specials = ['.', '+', '?', '$', '{', '}', '(', ')', '[', ']', '\\', '/'];
    var specialsRegex = new RegExp("[".concat(specials.join('\\'), "]"), 'g');
    return str.replace(specialsRegex, '\\$&');
  };

  /**
   * Converts :matches-css() arg property value match to regexp.
   *
   * @param rawValue Style match value pattern.
   *
   * @returns Arg of :matches-css() converted to regular expression.
   */
  var convertStyleMatchValueToRegexp = function convertStyleMatchValueToRegexp(rawValue) {
    var value;
    if (rawValue.startsWith(SLASH) && rawValue.endsWith(SLASH)) {
      // For regex patterns double quotes `"` and backslashes `\` should be escaped
      value = addUrlQuotesTo.regexpArg(rawValue);
      value = value.slice(1, -1);
    } else {
      // For non-regex patterns parentheses `(` `)` and square brackets `[` `]`
      // should be unescaped, because their escaping in filter rules is required
      value = addUrlQuotesTo.noneRegexpArg(rawValue);
      value = value.replace(/\\([\\()[\]"])/g, '$1');
      value = escapeRegExp(value);
      // e.g. div:matches-css(background-image: url(data:*))
      value = replaceAll(value, ASTERISK, REGEXP_ANY_SYMBOL);
    }
    return new RegExp(value, 'i');
  };

  /**
   * Makes some properties values compatible.
   *
   * @param propertyName Name of style property.
   * @param propertyValue Value of style property.
   *
   * @returns Normalized values for some CSS properties.
   */
  var normalizePropertyValue = function normalizePropertyValue(propertyName, propertyValue) {
    var normalized = '';
    switch (propertyName) {
      case CSS_PROPERTY.BACKGROUND:
      case CSS_PROPERTY.BACKGROUND_IMAGE:
        // sometimes url property does not have quotes
        // so we add them for consistent matching
        normalized = addUrlPropertyQuotes(propertyValue);
        break;
      case CSS_PROPERTY.CONTENT:
        normalized = removeContentQuotes(propertyValue);
        break;
      case CSS_PROPERTY.OPACITY:
        // https://bugs.webkit.org/show_bug.cgi?id=93445
        normalized = isSafariBrowser ? (Math.round(parseFloat(propertyValue) * 100) / 100).toString() : propertyValue;
        break;
      default:
        normalized = propertyValue;
    }
    return normalized;
  };

  /**
   * Returns domElement style property value
   * by css property name and standard pseudo-element.
   *
   * @param domElement DOM element.
   * @param propertyName CSS property name.
   * @param regularPseudoElement Standard pseudo-element — '::before', '::after' etc.
   *
   * @returns String containing the value of a specified CSS property.
   */
  var getComputedStylePropertyValue = function getComputedStylePropertyValue(domElement, propertyName, regularPseudoElement) {
    var style = window.getComputedStyle(domElement, regularPseudoElement);
    var propertyValue = style.getPropertyValue(propertyName);
    return normalizePropertyValue(propertyName, propertyValue);
  };
  /**
   * Parses arg of absolute pseudo-class into 'name' and 'value' if set.
   *
   * Used for :matches-css() - with COLON as separator,
   * for :matches-attr() and :matches-property() - with EQUAL_SIGN as separator.
   *
   * @param pseudoArg Arg of pseudo-class.
   * @param separator Divider symbol.
   *
   * @returns Parsed 'matches' pseudo-class arg data.
   */
  var getPseudoArgData = function getPseudoArgData(pseudoArg, separator) {
    var index = pseudoArg.indexOf(separator);
    var name;
    var value;
    if (index > -1) {
      name = pseudoArg.substring(0, index).trim();
      value = pseudoArg.substring(index + 1).trim();
    } else {
      name = pseudoArg;
    }
    return {
      name: name,
      value: value
    };
  };
  /**
   * Parses :matches-css() pseudo-class arg
   * where regular pseudo-element can be a part of arg
   * e.g. 'div:matches-css(before, color: rgb(255, 255, 255))'    <-- obsolete `:matches-css-before()`.
   *
   * @param pseudoName Pseudo-class name.
   * @param rawArg Pseudo-class arg.
   *
   * @returns Parsed :matches-css() pseudo-class arg data.
   * @throws An error on invalid `rawArg`.
   */
  var parseStyleMatchArg = function parseStyleMatchArg(pseudoName, rawArg) {
    var _getPseudoArgData = getPseudoArgData(rawArg, COMMA),
      name = _getPseudoArgData.name,
      value = _getPseudoArgData.value;
    var regularPseudoElement = name;
    var styleMatchArg = value;

    // check whether the string part before the separator is valid regular pseudo-element,
    // otherwise `regularPseudoElement` is null, and `styleMatchArg` is rawArg
    if (!Object.values(REGULAR_PSEUDO_ELEMENTS).includes(name)) {
      regularPseudoElement = null;
      styleMatchArg = rawArg;
    }
    if (!styleMatchArg) {
      throw new Error("Required style property argument part is missing in :".concat(pseudoName, "() arg: '").concat(rawArg, "'"));
    }

    // if regularPseudoElement is not `null`
    if (regularPseudoElement) {
      // pseudo-element should have two colon marks for Window.getComputedStyle() due to the syntax:
      // https://www.w3.org/TR/selectors-4/#pseudo-element-syntax
      // ':matches-css(before, content: ads)' ->> '::before'
      regularPseudoElement = "".concat(COLON).concat(COLON).concat(regularPseudoElement);
    }
    return {
      regularPseudoElement: regularPseudoElement,
      styleMatchArg: styleMatchArg
    };
  };

  /**
   * Checks whether the domElement is matched by :matches-css() arg.
   *
   * @param argsData Pseudo-class name, arg, and dom element to check.
   *
   @returns True if DOM element is matched.
   * @throws An error on invalid pseudo-class arg.
   */
  var isStyleMatched = function isStyleMatched(argsData) {
    var pseudoName = argsData.pseudoName,
      pseudoArg = argsData.pseudoArg,
      domElement = argsData.domElement;
    var _parseStyleMatchArg = parseStyleMatchArg(pseudoName, pseudoArg),
      regularPseudoElement = _parseStyleMatchArg.regularPseudoElement,
      styleMatchArg = _parseStyleMatchArg.styleMatchArg;
    var _getPseudoArgData2 = getPseudoArgData(styleMatchArg, COLON),
      matchName = _getPseudoArgData2.name,
      matchValue = _getPseudoArgData2.value;
    if (!matchName || !matchValue) {
      throw new Error("Required property name or value is missing in :".concat(pseudoName, "() arg: '").concat(styleMatchArg, "'"));
    }
    var valueRegexp;
    try {
      valueRegexp = convertStyleMatchValueToRegexp(matchValue);
    } catch (e) {
      logger.error(getErrorMessage(e));
      throw new Error("Invalid argument of :".concat(pseudoName, "() pseudo-class: '").concat(styleMatchArg, "'"));
    }
    var value = getComputedStylePropertyValue(domElement, matchName, regularPseudoElement);
    return valueRegexp && valueRegexp.test(value);
  };

  /**
   * Validates string arg for :matches-attr() and :matches-property().
   *
   * @param arg Pseudo-class arg.
   *
   * @returns True if 'matches' pseudo-class string arg is valid.
   */
  var validateStrMatcherArg = function validateStrMatcherArg(arg) {
    if (arg.includes(SLASH)) {
      return false;
    }
    if (!/^[\w-]+$/.test(arg)) {
      return false;
    }
    return true;
  };

  /**
   * Returns valid arg for :matches-attr() and :matcher-property().
   *
   * @param rawArg Arg pattern.
   * @param isWildcardAllowed Flag for wildcard (`*`) using as pseudo-class arg (defaults to false).
   *
   * @returns Valid arg for :matches-attr() and :matcher-property().
   * @throws An error on invalid `rawArg`.
   */
  var getValidMatcherArg = function getValidMatcherArg(rawArg) {
    var isWildcardAllowed = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : false;
    // if rawArg is missing for pseudo-class
    // e.g. :matches-attr()
    // error will be thrown before getValidMatcherArg() is called:
    // name or arg is missing in AbsolutePseudoClass

    var arg;
    if (rawArg.length > 1 && rawArg.startsWith(DOUBLE_QUOTE) && rawArg.endsWith(DOUBLE_QUOTE)) {
      rawArg = rawArg.slice(1, -1);
    }
    if (rawArg === '') {
      // e.g. :matches-property("")
      throw new Error('Argument should be specified. Empty arg is invalid.');
    }
    if (rawArg.startsWith(SLASH) && rawArg.endsWith(SLASH)) {
      // e.g. :matches-property("//")
      if (rawArg.length > 2) {
        arg = toRegExp(rawArg);
      } else {
        throw new Error("Invalid regexp: '".concat(rawArg, "'"));
      }
    } else if (rawArg.includes(ASTERISK)) {
      if (rawArg === ASTERISK && !isWildcardAllowed) {
        // e.g. :matches-attr(*)
        throw new Error("Argument should be more specific than ".concat(rawArg));
      }
      arg = replaceAll(rawArg, ASTERISK, REGEXP_ANY_SYMBOL);
      arg = new RegExp(arg);
    } else {
      if (!validateStrMatcherArg(rawArg)) {
        throw new Error("Invalid argument: '".concat(rawArg, "'"));
      }
      arg = rawArg;
    }
    return arg;
  };
  /**
   * Parses pseudo-class argument and returns parsed data.
   *
   * @param pseudoName Extended pseudo-class name.
   * @param pseudoArg Extended pseudo-class argument.
   *
   * @returns Parsed pseudo-class argument data.
   * @throws An error if attribute name is missing in pseudo-class arg.
   */
  var getRawMatchingData = function getRawMatchingData(pseudoName, pseudoArg) {
    var _getPseudoArgData3 = getPseudoArgData(pseudoArg, EQUAL_SIGN),
      rawName = _getPseudoArgData3.name,
      rawValue = _getPseudoArgData3.value;
    if (!rawName) {
      throw new Error("Required attribute name is missing in :".concat(pseudoName, " arg: ").concat(pseudoArg));
    }
    return {
      rawName: rawName,
      rawValue: rawValue
    };
  };

  /**
   * Checks whether the domElement is matched by :matches-attr() arg.
   *
   * @param argsData Pseudo-class name, arg, and dom element to check.
   *
   @returns True if DOM element is matched.
   * @throws An error on invalid arg of pseudo-class.
   */
  var isAttributeMatched = function isAttributeMatched(argsData) {
    var pseudoName = argsData.pseudoName,
      pseudoArg = argsData.pseudoArg,
      domElement = argsData.domElement;
    var elementAttributes = domElement.attributes;
    // no match if dom element has no attributes
    if (elementAttributes.length === 0) {
      return false;
    }
    var _getRawMatchingData = getRawMatchingData(pseudoName, pseudoArg),
      rawAttrName = _getRawMatchingData.rawName,
      rawAttrValue = _getRawMatchingData.rawValue;
    var attrNameMatch;
    try {
      attrNameMatch = getValidMatcherArg(rawAttrName);
    } catch (e) {
      var errorMessage = getErrorMessage(e);
      logger.error(errorMessage);
      throw new SyntaxError(errorMessage);
    }
    var isMatched = false;
    var i = 0;
    while (i < elementAttributes.length && !isMatched) {
      var attr = elementAttributes[i];
      if (!attr) {
        break;
      }
      var isNameMatched = attrNameMatch instanceof RegExp ? attrNameMatch.test(attr.name) : attrNameMatch === attr.name;
      if (!rawAttrValue) {
        // for rules with no attribute value specified
        // e.g. :matches-attr("/regex/") or :matches-attr("attr-name")
        isMatched = isNameMatched;
      } else {
        var attrValueMatch = void 0;
        try {
          attrValueMatch = getValidMatcherArg(rawAttrValue);
        } catch (e) {
          var _errorMessage = getErrorMessage(e);
          logger.error(_errorMessage);
          throw new SyntaxError(_errorMessage);
        }
        var isValueMatched = attrValueMatch instanceof RegExp ? attrValueMatch.test(attr.value) : attrValueMatch === attr.value;
        isMatched = isNameMatched && isValueMatched;
      }
      i += 1;
    }
    return isMatched;
  };

  /**
   * Parses raw :matches-property() arg which may be chain of properties.
   *
   * @param input Argument of :matches-property().
   *
   * @returns Arg of :matches-property() as array of strings or regular expressions.
   * @throws An error on invalid chain.
   */
  var parseRawPropChain = function parseRawPropChain(input) {
    if (input.length > 1 && input.startsWith(DOUBLE_QUOTE) && input.endsWith(DOUBLE_QUOTE)) {
      input = input.slice(1, -1);
    }
    var chainChunks = input.split(DOT);
    var chainPatterns = [];
    var patternBuffer = '';
    var isRegexpPattern = false;
    var i = 0;
    while (i < chainChunks.length) {
      var chunk = getItemByIndex(chainChunks, i, "Invalid pseudo-class arg: '".concat(input, "'"));
      if (chunk.startsWith(SLASH) && chunk.endsWith(SLASH) && chunk.length > 2) {
        // regexp pattern with no dot in it, e.g. /propName/
        chainPatterns.push(chunk);
      } else if (chunk.startsWith(SLASH)) {
        // if chunk is a start of regexp pattern
        isRegexpPattern = true;
        patternBuffer += chunk;
      } else if (chunk.endsWith(SLASH)) {
        isRegexpPattern = false;
        // restore dot removed while splitting
        // e.g. testProp./.{1,5}/
        patternBuffer += ".".concat(chunk);
        chainPatterns.push(patternBuffer);
        patternBuffer = '';
      } else {
        // if there are few dots in regexp pattern
        // so chunk might be in the middle of it
        if (isRegexpPattern) {
          patternBuffer += chunk;
        } else {
          // otherwise it is string pattern
          chainPatterns.push(chunk);
        }
      }
      i += 1;
    }
    if (patternBuffer.length > 0) {
      throw new Error("Invalid regexp property pattern '".concat(input, "'"));
    }
    var chainMatchPatterns = chainPatterns.map(function (pattern) {
      if (pattern.length === 0) {
        // e.g. '.prop.id' or 'nested..test'
        throw new Error("Empty pattern '".concat(pattern, "' is invalid in chain '").concat(input, "'"));
      }
      var validPattern;
      try {
        validPattern = getValidMatcherArg(pattern, true);
      } catch (e) {
        logger.error(getErrorMessage(e));
        throw new Error("Invalid property pattern '".concat(pattern, "' in property chain '").concat(input, "'"));
      }
      return validPattern;
    });
    return chainMatchPatterns;
  };
  /**
   * Checks if the property exists in the base object (recursively).
   *
   * @param base Element to check.
   * @param chain Array of objects - parsed string property chain.
   * @param output Result accumulator (defaults to []).
   *
   * @returns Array of parsed data — representation of `base`-related `chain`.
   */
  var _filterRootsByRegexpChain = function filterRootsByRegexpChain(base, chain) {
    var output = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : [];
    var tempProp = getFirst(chain);
    if (chain.length === 1) {
      var key;
      for (key in base) {
        if (tempProp instanceof RegExp) {
          if (tempProp.test(key)) {
            output.push({
              base: base,
              prop: key,
              value: base[key]
            });
          }
        } else if (tempProp === key) {
          output.push({
            base: base,
            prop: tempProp,
            value: base[key]
          });
        }
      }
      return output;
    }

    // if there is a regexp prop in input chain
    // e.g. 'unit./^ad.+/.src' for 'unit.ad-1gf2.src unit.ad-fgd34.src'),
    // every base keys should be tested by regexp and it can be more that one results
    if (tempProp instanceof RegExp) {
      var nextProp = chain.slice(1);
      var baseKeys = [];
      for (var _key in base) {
        if (tempProp.test(_key)) {
          baseKeys.push(_key);
        }
      }
      baseKeys.forEach(function (key) {
        var _Object$getOwnPropert;
        var item = (_Object$getOwnPropert = Object.getOwnPropertyDescriptor(base, key)) === null || _Object$getOwnPropert === void 0 ? void 0 : _Object$getOwnPropert.value;
        _filterRootsByRegexpChain(item, nextProp, output);
      });
    }
    if (base && typeof tempProp === 'string') {
      var _Object$getOwnPropert2;
      var nextBase = (_Object$getOwnPropert2 = Object.getOwnPropertyDescriptor(base, tempProp)) === null || _Object$getOwnPropert2 === void 0 ? void 0 : _Object$getOwnPropert2.value;
      chain = chain.slice(1);
      if (nextBase !== undefined) {
        _filterRootsByRegexpChain(nextBase, chain, output);
      }
    }
    return output;
  };

  /**
   * Checks whether the domElement is matched by :matches-property() arg.
   *
   * @param argsData Pseudo-class name, arg, and dom element to check.
   *
   @returns True if DOM element is matched.
   * @throws An error on invalid prop in chain.
   */
  var isPropertyMatched = function isPropertyMatched(argsData) {
    var pseudoName = argsData.pseudoName,
      pseudoArg = argsData.pseudoArg,
      domElement = argsData.domElement;
    var _getRawMatchingData2 = getRawMatchingData(pseudoName, pseudoArg),
      rawPropertyName = _getRawMatchingData2.rawName,
      rawPropertyValue = _getRawMatchingData2.rawValue;

    // chained property name cannot include '/' or '.'
    // so regex prop names with such escaped characters are invalid
    if (rawPropertyName.includes('\\/') || rawPropertyName.includes('\\.')) {
      throw new Error("Invalid :".concat(pseudoName, " name pattern: ").concat(rawPropertyName));
    }
    var propChainMatches;
    try {
      propChainMatches = parseRawPropChain(rawPropertyName);
    } catch (e) {
      var errorMessage = getErrorMessage(e);
      logger.error(errorMessage);
      throw new SyntaxError(errorMessage);
    }
    var ownerObjArr = _filterRootsByRegexpChain(domElement, propChainMatches);
    if (ownerObjArr.length === 0) {
      return false;
    }
    var isMatched = true;
    if (rawPropertyValue) {
      var propValueMatch;
      try {
        propValueMatch = getValidMatcherArg(rawPropertyValue);
      } catch (e) {
        var _errorMessage2 = getErrorMessage(e);
        logger.error(_errorMessage2);
        throw new SyntaxError(_errorMessage2);
      }
      if (propValueMatch) {
        for (var i = 0; i < ownerObjArr.length; i += 1) {
          var _ownerObjArr$i;
          var realValue = (_ownerObjArr$i = ownerObjArr[i]) === null || _ownerObjArr$i === void 0 ? void 0 : _ownerObjArr$i.value;
          if (propValueMatch instanceof RegExp) {
            isMatched = propValueMatch.test(convertTypeIntoString(realValue));
          } else {
            // handle 'null' and 'undefined' property values set as string
            if (realValue === 'null' || realValue === 'undefined') {
              isMatched = propValueMatch === realValue;
              break;
            }
            isMatched = convertTypeFromString(propValueMatch) === realValue;
          }
          if (isMatched) {
            break;
          }
        }
      }
    }
    return isMatched;
  };

  /**
   * Checks whether the textContent is matched by :contains arg.
   *
   * @param argsData Pseudo-class name, arg, and dom element to check.
   *
   @returns True if DOM element is matched.
   * @throws An error on invalid arg of pseudo-class.
   */
  var isTextMatched = function isTextMatched(argsData) {
    var pseudoName = argsData.pseudoName,
      pseudoArg = argsData.pseudoArg,
      domElement = argsData.domElement;
    var textContent = getNodeTextContent(domElement);
    var isTextContentMatched;
    var pseudoArgToMatch = pseudoArg;
    if (pseudoArgToMatch.startsWith(SLASH) && REGEXP_WITH_FLAGS_REGEXP.test(pseudoArgToMatch)) {
      // regexp arg
      var flagsIndex = pseudoArgToMatch.lastIndexOf('/');
      var flagsStr = pseudoArgToMatch.substring(flagsIndex + 1);
      pseudoArgToMatch = pseudoArgToMatch.substring(0, flagsIndex + 1).slice(1, -1).replace(/\\([\\"])/g, '$1');
      var regex;
      try {
        regex = new RegExp(pseudoArgToMatch, flagsStr);
      } catch (e) {
        throw new Error("Invalid argument of :".concat(pseudoName, "() pseudo-class: ").concat(pseudoArg));
      }
      isTextContentMatched = regex.test(textContent);
    } else {
      // none-regexp arg
      pseudoArgToMatch = pseudoArgToMatch.replace(/\\([\\()[\]"])/g, '$1');
      isTextContentMatched = textContent.includes(pseudoArgToMatch);
    }
    return isTextContentMatched;
  };

  /**
   * Validates number arg for :nth-ancestor() and :upward() pseudo-classes.
   *
   * @param rawArg Raw arg of pseudo-class.
   * @param pseudoName Pseudo-class name.
   *
   * @returns Valid number arg for :nth-ancestor() and :upward().
   * @throws An error on invalid `rawArg`.
   */
  var getValidNumberAncestorArg = function getValidNumberAncestorArg(rawArg, pseudoName) {
    var deep = Number(rawArg);
    if (Number.isNaN(deep) || deep < 1 || deep >= 256) {
      throw new Error("Invalid argument of :".concat(pseudoName, " pseudo-class: '").concat(rawArg, "'"));
    }
    return deep;
  };

  /**
   * Returns nth ancestor by 'deep' number arg OR undefined if ancestor range limit exceeded.
   *
   * @param domElement DOM element to find ancestor for.
   * @param nth Depth up to needed ancestor.
   * @param pseudoName Pseudo-class name.
   *
   * @returns Ancestor element found in DOM, or null if not found.
   * @throws An error on invalid `nth` arg.
   */
  var getNthAncestor = function getNthAncestor(domElement, nth, pseudoName) {
    var ancestor = null;
    var i = 0;
    while (i < nth) {
      ancestor = domElement.parentElement;
      if (!ancestor) {
        throw new Error("Out of DOM: Argument of :".concat(pseudoName, "() pseudo-class is too big \u2014 '").concat(nth, "'."));
      }
      domElement = ancestor;
      i += 1;
    }
    return ancestor;
  };

  /**
   * Validates standard CSS selector.
   *
   * @param selector Standard selector.
   *
   * @returns True if standard CSS selector is valid.
   */
  var validateStandardSelector = function validateStandardSelector(selector) {
    var isValid;
    try {
      document.querySelectorAll(selector);
      isValid = true;
    } catch (e) {
      isValid = false;
    }
    return isValid;
  };

  /**
   * Wrapper to run matcher `callback` with `args`
   * and throw error with `errorMessage` if `callback` run fails.
   *
   * @param callback Matcher callback.
   * @param argsData Args needed for matcher callback.
   * @param errorMessage Error message.
   *
   * @returns True if `callback` returns true.
   * @throws An error if `callback` fails.
   */
  var matcherWrapper = function matcherWrapper(callback, argsData, errorMessage) {
    var isMatched;
    try {
      isMatched = callback(argsData);
    } catch (e) {
      logger.error(getErrorMessage(e));
      throw new Error(errorMessage);
    }
    return isMatched;
  };

  /**
   * Generates common error message to throw while matching element `propDesc`.
   *
   * @param propDesc Text to describe what element 'prop' pseudo-class is trying to match.
   * @param pseudoName Pseudo-class name.
   * @param pseudoArg Pseudo-class arg.
   *
   * @returns Generated error message string.
   */
  var getAbsolutePseudoError = function getAbsolutePseudoError(propDesc, pseudoName, pseudoArg) {
    // eslint-disable-next-line max-len
    return "".concat(MATCHING_ELEMENT_ERROR_PREFIX, " ").concat(propDesc, ", may be invalid :").concat(pseudoName, "() pseudo-class arg: '").concat(pseudoArg, "'");
  };

  /**
   * Checks whether the domElement is matched by absolute extended pseudo-class argument.
   *
   * @param domElement Page element.
   * @param pseudoName Pseudo-class name.
   * @param pseudoArg Pseudo-class arg.
   *
   * @returns True if `domElement` is matched by absolute pseudo-class.
   * @throws An error on unknown absolute pseudo-class.
   */
  var isMatchedByAbsolutePseudo = function isMatchedByAbsolutePseudo(domElement, pseudoName, pseudoArg) {
    var argsData;
    var errorMessage;
    var callback;
    switch (pseudoName) {
      case CONTAINS_PSEUDO:
      case HAS_TEXT_PSEUDO:
      case ABP_CONTAINS_PSEUDO:
        callback = isTextMatched;
        argsData = {
          pseudoName: pseudoName,
          pseudoArg: pseudoArg,
          domElement: domElement
        };
        errorMessage = getAbsolutePseudoError('text content', pseudoName, pseudoArg);
        break;
      case MATCHES_CSS_PSEUDO:
      case MATCHES_CSS_AFTER_PSEUDO:
      case MATCHES_CSS_BEFORE_PSEUDO:
        callback = isStyleMatched;
        argsData = {
          pseudoName: pseudoName,
          pseudoArg: pseudoArg,
          domElement: domElement
        };
        errorMessage = getAbsolutePseudoError('style', pseudoName, pseudoArg);
        break;
      case MATCHES_ATTR_PSEUDO_CLASS_MARKER:
        callback = isAttributeMatched;
        argsData = {
          domElement: domElement,
          pseudoName: pseudoName,
          pseudoArg: pseudoArg
        };
        errorMessage = getAbsolutePseudoError('attributes', pseudoName, pseudoArg);
        break;
      case MATCHES_PROPERTY_PSEUDO_CLASS_MARKER:
        callback = isPropertyMatched;
        argsData = {
          domElement: domElement,
          pseudoName: pseudoName,
          pseudoArg: pseudoArg
        };
        errorMessage = getAbsolutePseudoError('properties', pseudoName, pseudoArg);
        break;
      default:
        throw new Error("Unknown absolute pseudo-class :".concat(pseudoName, "()"));
    }
    return matcherWrapper(callback, argsData, errorMessage);
  };
  var findByAbsolutePseudoPseudo = {
    /**
     * Returns list of nth ancestors relative to every dom node from domElements list.
     *
     * @param domElements DOM elements.
     * @param rawPseudoArg Number arg of :nth-ancestor() or :upward() pseudo-class.
     * @param pseudoName Pseudo-class name.
     *
     * @returns Array of ancestor DOM elements.
     */
    nthAncestor: function nthAncestor(domElements, rawPseudoArg, pseudoName) {
      var deep = getValidNumberAncestorArg(rawPseudoArg, pseudoName);
      var ancestors = domElements.map(function (domElement) {
        var ancestor = null;
        try {
          ancestor = getNthAncestor(domElement, deep, pseudoName);
        } catch (e) {
          logger.error(getErrorMessage(e));
        }
        return ancestor;
      }).filter(isHtmlElement);
      return ancestors;
    },
    /**
     * Returns list of elements by xpath expression, evaluated on every dom node from domElements list.
     *
     * @param domElements DOM elements.
     * @param rawPseudoArg Arg of :xpath() pseudo-class.
     *
     * @returns Array of DOM elements matched by xpath expression.
     */
    xpath: function xpath(domElements, rawPseudoArg) {
      var foundElements = domElements.map(function (domElement) {
        var result = [];
        var xpathResult;
        try {
          xpathResult = document.evaluate(rawPseudoArg, domElement, null, window.XPathResult.UNORDERED_NODE_ITERATOR_TYPE, null);
        } catch (e) {
          logger.error(getErrorMessage(e));
          throw new Error("Invalid argument of :xpath() pseudo-class: '".concat(rawPseudoArg, "'"));
        }
        var node = xpathResult.iterateNext();
        while (node) {
          if (isHtmlElement(node)) {
            result.push(node);
          }
          node = xpathResult.iterateNext();
        }
        return result;
      });
      return flatten(foundElements);
    },
    /**
     * Returns list of closest ancestors relative to every dom node from domElements list.
     *
     * @param domElements DOM elements.
     * @param rawPseudoArg Standard selector arg of :upward() pseudo-class.
     *
     * @returns Array of closest ancestor DOM elements.
     * @throws An error if `rawPseudoArg` is not a valid standard selector.
     */
    upward: function upward(domElements, rawPseudoArg) {
      if (!validateStandardSelector(rawPseudoArg)) {
        throw new Error("Invalid argument of :upward pseudo-class: '".concat(rawPseudoArg, "'"));
      }
      var closestAncestors = domElements.map(function (domElement) {
        // closest to parent element should be found
        // otherwise `.base:upward(.base)` will return itself too, not only ancestor
        var parent = domElement.parentElement;
        if (!parent) {
          return null;
        }
        return parent.closest(rawPseudoArg);
      }).filter(isHtmlElement);
      return closestAncestors;
    }
  };

  /**
   * Calculated selector text which is needed to :has(), :is() and :not() pseudo-classes.
   * Contains calculated part (depends on the processed element)
   * and value of RegularSelector which is next to selector by.
   *
   * Native Document.querySelectorAll() does not select exact descendant elements
   * but match all page elements satisfying the selector,
   * so extra specification is needed for proper descendants selection
   * e.g. 'div:has(> img)'.
   *
   * Its calculation depends on extended selector.
   */

  /**
   * Combined `:scope` pseudo-class and **child** combinator — `:scope>`.
   */
  var scopeDirectChildren = "".concat(SCOPE_CSS_PSEUDO_CLASS).concat(CHILD_COMBINATOR);

  /**
   * Combined `:scope` pseudo-class and **descendant** combinator — `:scope `.
   */
  var scopeAnyChildren = "".concat(SCOPE_CSS_PSEUDO_CLASS).concat(DESCENDANT_COMBINATOR);

  /**
   * Type for relative pseudo-class helpers args.
   */

  /**
   * Returns the first of RegularSelector child node for `selectorNode`.
   *
   * @param selectorNode Ast Selector node.
   * @param pseudoName Name of relative pseudo-class.
   *
   * @returns Ast RegularSelector node.
   */
  var getFirstInnerRegularChild = function getFirstInnerRegularChild(selectorNode, pseudoName) {
    return getFirstRegularChild(selectorNode.children, "RegularSelector is missing for :".concat(pseudoName, "() pseudo-class"));
  };

  // TODO: fix for <forgiving-relative-selector-list>
  // https://github.com/AdguardTeam/ExtendedCss/issues/154
  /**
   * Checks whether the element has all relative elements specified by pseudo-class arg.
   * Used for :has() pseudo-class.
   *
   * @param argsData Relative pseudo-class helpers args data.
   *
   * @returns True if **all selectors** from argsData.relativeSelectorList is **matched** for argsData.element.
   */
  var hasRelativesBySelectorList = function hasRelativesBySelectorList(argsData) {
    var element = argsData.element,
      relativeSelectorList = argsData.relativeSelectorList,
      pseudoName = argsData.pseudoName;
    return relativeSelectorList.children
    // Array.every() is used here as each Selector node from SelectorList should exist on page
    .every(function (selectorNode) {
      // selectorList.children always starts with regular selector as any selector generally
      var relativeRegularSelector = getFirstInnerRegularChild(selectorNode, pseudoName);
      var specifiedSelector = '';
      var rootElement = null;
      var regularSelector = getNodeValue(relativeRegularSelector);
      if (regularSelector.startsWith(NEXT_SIBLING_COMBINATOR) || regularSelector.startsWith(SUBSEQUENT_SIBLING_COMBINATOR)) {
        /**
         * For matching the element by "element:has(+ next-sibling)" and "element:has(~ sibling)"
         * we check whether the element's parentElement has specific direct child combination,
         * e.g. 'h1:has(+ .share)' -> `h1Node.parentElement.querySelectorAll(':scope > h1 + .share')`.
         *
         * @see {@link https://www.w3.org/TR/selectors-4/#relational}
         */
        rootElement = element.parentElement;
        var elementSelectorText = getElementSelectorDesc(element);
        specifiedSelector = "".concat(scopeDirectChildren).concat(elementSelectorText).concat(regularSelector);
      } else if (regularSelector === ASTERISK) {
        /**
         * :scope specification is needed for proper descendants selection
         * as native element.querySelectorAll() does not select exact element descendants
         * e.g. 'a:has(> img)' -> `aNode.querySelectorAll(':scope > img')`.
         *
         * For 'any selector' as arg of relative simplicity should be set for all inner elements
         * e.g. 'div:has(*)' -> `divNode.querySelectorAll(':scope *')`
         * which means empty div with no child element.
         */
        rootElement = element;
        specifiedSelector = "".concat(scopeAnyChildren).concat(ASTERISK);
      } else {
        /**
         * As it described above, inner elements should be found using `:scope` pseudo-class
         * e.g. 'a:has(> img)' -> `aNode.querySelectorAll(':scope > img')`
         * OR '.block(div > span)' -> `blockClassNode.querySelectorAll(':scope div > span')`.
         */
        specifiedSelector = "".concat(scopeAnyChildren).concat(regularSelector);
        rootElement = element;
      }
      if (!rootElement) {
        throw new Error("Selection by :".concat(pseudoName, "() pseudo-class is not possible"));
      }
      var relativeElements;
      try {
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        relativeElements = getElementsForSelectorNode(selectorNode, rootElement, specifiedSelector);
      } catch (e) {
        logger.error(getErrorMessage(e));
        // fail for invalid selector
        throw new Error("Invalid selector for :".concat(pseudoName, "() pseudo-class: '").concat(regularSelector, "'"));
      }
      return relativeElements.length > 0;
    });
  };

  /**
   * Checks whether the element is an any element specified by pseudo-class arg.
   * Used for :is() pseudo-class.
   *
   * @param argsData Relative pseudo-class helpers args data.
   *
   * @returns True if **any selector** from argsData.relativeSelectorList is **matched** for argsData.element.
   */
  var isAnyElementBySelectorList = function isAnyElementBySelectorList(argsData) {
    var element = argsData.element,
      relativeSelectorList = argsData.relativeSelectorList,
      pseudoName = argsData.pseudoName;
    return relativeSelectorList.children
    // Array.some() is used here as any selector from selector list should exist on page
    .some(function (selectorNode) {
      // selectorList.children always starts with regular selector
      var relativeRegularSelector = getFirstInnerRegularChild(selectorNode, pseudoName);

      /**
       * For checking the element by 'div:is(.banner)'
       * we check whether the element's parentElement has any specific direct child.
       */
      var rootElement = getParent(element, "Selection by :".concat(pseudoName, "() pseudo-class is not possible"));

      /**
       * So we calculate the element "description" by it's tagname and attributes for targeting
       * and use it to specify the selection
       * e.g. `div:is(.banner)` --> `divNode.parentElement.querySelectorAll(':scope > .banner')`.
       */
      var specifiedSelector = "".concat(scopeDirectChildren).concat(getNodeValue(relativeRegularSelector));
      var anyElements;
      try {
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        anyElements = getElementsForSelectorNode(selectorNode, rootElement, specifiedSelector);
      } catch (e) {
        // do not fail on invalid selectors for :is()
        return false;
      }

      // TODO: figure out how to handle complex selectors with extended pseudo-classes
      // (check readme - extended-css-is-limitations)
      // because `element` and `anyElements` may be from different DOM levels
      return anyElements.includes(element);
    });
  };

  /**
   * Checks whether the element is not an element specified by pseudo-class arg.
   * Used for :not() pseudo-class.
   *
   * @param argsData Relative pseudo-class helpers args data.
   *
   * @returns True if **any selector** from argsData.relativeSelectorList is **not matched** for argsData.element.
   */
  var notElementBySelectorList = function notElementBySelectorList(argsData) {
    var element = argsData.element,
      relativeSelectorList = argsData.relativeSelectorList,
      pseudoName = argsData.pseudoName;
    return relativeSelectorList.children
    // Array.every() is used here as element should not be selected by any selector from selector list
    .every(function (selectorNode) {
      // selectorList.children always starts with regular selector
      var relativeRegularSelector = getFirstInnerRegularChild(selectorNode, pseudoName);

      /**
       * For checking the element by 'div:not([data="content"])
       * we check whether the element's parentElement has any specific direct child.
       */
      var rootElement = getParent(element, "Selection by :".concat(pseudoName, "() pseudo-class is not possible"));

      /**
       * So we calculate the element "description" by it's tagname and attributes for targeting
       * and use it to specify the selection
       * e.g. `div:not(.banner)` --> `divNode.parentElement.querySelectorAll(':scope > .banner')`.
       */
      var specifiedSelector = "".concat(scopeDirectChildren).concat(getNodeValue(relativeRegularSelector));
      var anyElements;
      try {
        // eslint-disable-next-line @typescript-eslint/no-use-before-define
        anyElements = getElementsForSelectorNode(selectorNode, rootElement, specifiedSelector);
      } catch (e) {
        // fail on invalid selectors for :not()
        logger.error(getErrorMessage(e));
        // eslint-disable-next-line max-len
        throw new Error("Invalid selector for :".concat(pseudoName, "() pseudo-class: '").concat(getNodeValue(relativeRegularSelector), "'"));
      }

      // TODO: figure out how to handle up-looking pseudo-classes inside :not()
      // (check readme - extended-css-not-limitations)
      // because `element` and `anyElements` may be from different DOM levels
      return !anyElements.includes(element);
    });
  };

  /**
   * Selects dom elements by value of RegularSelector.
   *
   * @param regularSelectorNode RegularSelector node.
   * @param root Root DOM element.
   * @param specifiedSelector {@link SpecifiedSelector}.
   *
   * @returns Array of DOM elements.
   * @throws An error if RegularSelector node value is an invalid selector.
   */
  var getByRegularSelector = function getByRegularSelector(regularSelectorNode, root, specifiedSelector) {
    var selectorText = specifiedSelector ? specifiedSelector : getNodeValue(regularSelectorNode);
    var selectedElements = [];
    try {
      selectedElements = Array.from(root.querySelectorAll(selectorText));
    } catch (e) {
      throw new Error("Error: unable to select by '".concat(selectorText, "' \u2014 ").concat(getErrorMessage(e)));
    }
    return selectedElements;
  };

  /**
   * Returns list of dom elements filtered or selected by ExtendedSelector node.
   *
   * @param domElements Array of DOM elements.
   * @param extendedSelectorNode ExtendedSelector node.
   *
   * @returns Array of DOM elements.
   * @throws An error on unknown pseudo-class,
   * absent or invalid arg of extended pseudo-class, etc.
   */
  var getByExtendedSelector = function getByExtendedSelector(domElements, extendedSelectorNode) {
    var foundElements = [];
    var extendedPseudoClassNode = getPseudoClassNode(extendedSelectorNode);
    var pseudoName = getNodeName(extendedPseudoClassNode);
    if (isAbsolutePseudoClass(pseudoName)) {
      // absolute extended pseudo-classes should have an argument
      var absolutePseudoArg = getNodeValue(extendedPseudoClassNode, "Missing arg for :".concat(pseudoName, "() pseudo-class"));
      if (pseudoName === NTH_ANCESTOR_PSEUDO_CLASS_MARKER) {
        // :nth-ancestor()
        foundElements = findByAbsolutePseudoPseudo.nthAncestor(domElements, absolutePseudoArg, pseudoName);
      } else if (pseudoName === XPATH_PSEUDO_CLASS_MARKER) {
        // :xpath()
        try {
          document.createExpression(absolutePseudoArg, null);
        } catch (e) {
          throw new Error("Invalid argument of :".concat(pseudoName, "() pseudo-class: '").concat(absolutePseudoArg, "'"));
        }
        foundElements = findByAbsolutePseudoPseudo.xpath(domElements, absolutePseudoArg);
      } else if (pseudoName === UPWARD_PSEUDO_CLASS_MARKER) {
        // :upward()
        if (Number.isNaN(Number(absolutePseudoArg))) {
          // so arg is selector, not a number
          foundElements = findByAbsolutePseudoPseudo.upward(domElements, absolutePseudoArg);
        } else {
          foundElements = findByAbsolutePseudoPseudo.nthAncestor(domElements, absolutePseudoArg, pseudoName);
        }
      } else {
        // all other absolute extended pseudo-classes
        // e.g. contains, matches-attr, etc.
        foundElements = domElements.filter(function (element) {
          return isMatchedByAbsolutePseudo(element, pseudoName, absolutePseudoArg);
        });
      }
    } else if (isRelativePseudoClass(pseudoName)) {
      var relativeSelectorList = getRelativeSelectorListNode(extendedPseudoClassNode);
      var relativePredicate;
      switch (pseudoName) {
        case HAS_PSEUDO_CLASS_MARKER:
        case ABP_HAS_PSEUDO_CLASS_MARKER:
          relativePredicate = function relativePredicate(element) {
            return hasRelativesBySelectorList({
              element: element,
              relativeSelectorList: relativeSelectorList,
              pseudoName: pseudoName
            });
          };
          break;
        case IS_PSEUDO_CLASS_MARKER:
          relativePredicate = function relativePredicate(element) {
            return isAnyElementBySelectorList({
              element: element,
              relativeSelectorList: relativeSelectorList,
              pseudoName: pseudoName
            });
          };
          break;
        case NOT_PSEUDO_CLASS_MARKER:
          relativePredicate = function relativePredicate(element) {
            return notElementBySelectorList({
              element: element,
              relativeSelectorList: relativeSelectorList,
              pseudoName: pseudoName
            });
          };
          break;
        default:
          throw new Error("Unknown relative pseudo-class: '".concat(pseudoName, "'"));
      }
      foundElements = domElements.filter(relativePredicate);
    } else if (isStandalonePseudoClass(pseudoName)) {
      var standalonePredicate;
      switch (pseudoName) {
        case EMPTY_TRIMMED_PSEUDO_CLASS_MARKER:
          standalonePredicate = function standalonePredicate(element) {
            var _element$textContent;
            var textContent = (_element$textContent = element.textContent) !== null && _element$textContent !== void 0 ? _element$textContent : '';
            // String.prototype.trim() removes all Unicode whitespace per the ECMAScript spec,
            // including \u00A0 (non-breaking space / &nbsp;).
            // See https://tc39.es/ecma262/#sec-string.prototype.trim
            return textContent.trim().length === 0;
          };
          break;
        default:
          throw new Error("Unknown standalone pseudo-class: '".concat(pseudoName, "'"));
      }
      foundElements = domElements.filter(standalonePredicate);
    } else {
      // extra check is parser missed something
      throw new Error("Unknown extended pseudo-class: '".concat(pseudoName, "'"));
    }
    return foundElements;
  };

  /**
   * Returns list of dom elements which is selected by RegularSelector value.
   *
   * @param domElements Array of DOM elements.
   * @param regularSelectorNode RegularSelector node.
   *
   * @returns Array of DOM elements.
   * @throws An error if RegularSelector has not value.
   */
  var getByFollowingRegularSelector = function getByFollowingRegularSelector(domElements, regularSelectorNode) {
    // array of arrays because of Array.map() later
    var foundElements = [];
    var value = getNodeValue(regularSelectorNode);
    if (value.startsWith(CHILD_COMBINATOR)) {
      // e.g. div:has(> img) > .banner
      foundElements = domElements.map(function (root) {
        var specifiedSelector = "".concat(SCOPE_CSS_PSEUDO_CLASS).concat(value);
        return getByRegularSelector(regularSelectorNode, root, specifiedSelector);
      });
    } else if (value.startsWith(NEXT_SIBLING_COMBINATOR) || value.startsWith(SUBSEQUENT_SIBLING_COMBINATOR)) {
      // e.g. div:has(> img) + .banner
      // or   div:has(> img) ~ .banner
      foundElements = domElements.map(function (element) {
        var rootElement = element.parentElement;
        if (!rootElement) {
          // do not throw error if there in no parent for element
          // e.g. '*:contains(text)' selects `html` which has no parentElement
          return [];
        }
        var elementSelectorText = getElementSelectorDesc(element);
        var specifiedSelector = "".concat(scopeDirectChildren).concat(elementSelectorText).concat(value);
        var selected = getByRegularSelector(regularSelectorNode, rootElement, specifiedSelector);
        return selected;
      });
    } else {
      // space-separated regular selector after extended one
      // e.g. div:has(> img) .banner
      foundElements = domElements.map(function (root) {
        var specifiedSelector = "".concat(scopeAnyChildren).concat(getNodeValue(regularSelectorNode));
        return getByRegularSelector(regularSelectorNode, root, specifiedSelector);
      });
    }
    // foundElements should be flattened
    // as getByRegularSelector() returns elements array, and Array.map() collects them to array
    return flatten(foundElements);
  };

  /**
   * Returns elements nodes for Selector node.
   * As far as any selector always starts with regular part,
   * it selects by RegularSelector first and checks found elements later.
   *
   * Relative pseudo-classes has it's own subtree so getElementsForSelectorNode is called recursively.
   *
   * 'specifiedSelector' is needed for :has(), :is(), and :not() pseudo-classes
   * as native querySelectorAll() does not select exact element descendants even if it is called on 'div'
   * e.g. ':scope' specification is needed for proper descendants selection for 'div:has(> img)'.
   * So we check `divNode.querySelectorAll(':scope > img').length > 0`.
   *
   * @param selectorNode Selector node.
   * @param root Root DOM element.
   * @param specifiedSelector Needed element specification.
   *
   * @returns Array of DOM elements.
   * @throws An error if there is no selectorNodeChild.
   */
  var getElementsForSelectorNode = function getElementsForSelectorNode(selectorNode, root, specifiedSelector) {
    var selectedElements = [];
    var i = 0;
    while (i < selectorNode.children.length) {
      var selectorNodeChild = getItemByIndex(selectorNode.children, i, 'selectorNodeChild should be specified');
      if (i === 0) {
        // any selector always starts with regular selector
        selectedElements = getByRegularSelector(selectorNodeChild, root, specifiedSelector);
      } else if (isExtendedSelectorNode(selectorNodeChild)) {
        // filter previously selected elements by next selector nodes
        selectedElements = getByExtendedSelector(selectedElements, selectorNodeChild);
      } else if (isRegularSelectorNode(selectorNodeChild)) {
        selectedElements = getByFollowingRegularSelector(selectedElements, selectorNodeChild);
      }
      i += 1;
    }
    return selectedElements;
  };

  /**
   * Selects elements by ast.
   *
   * @param ast Ast of parsed selector.
   * @param doc Document.
   *
   * @returns Array of DOM elements.
   */
  var selectElementsByAst = function selectElementsByAst(ast) {
    var doc = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : document;
    var selectedElements = [];
    // ast root is SelectorList node;
    // it has Selector nodes as children which should be processed separately
    ast.children.forEach(function (selectorNode) {
      selectedElements.push.apply(selectedElements, _toConsumableArray(getElementsForSelectorNode(selectorNode, doc)));
    });
    // selectedElements should be flattened as it is array of arrays with elements
    var uniqueElements = _toConsumableArray(new Set(flatten(selectedElements)));
    return uniqueElements;
  };

  /**
   * Class of ExtCssDocument is needed for caching.
   * For making cache related to each new instance of class, not global.
   */
  var ExtCssDocument = /*#__PURE__*/function () {
    /**
     * Cache with selectors and their AST parsing results.
     */

    /**
     * Creates new ExtCssDocument and inits new `astCache`.
     */
    function ExtCssDocument() {
      _classCallCheck(this, ExtCssDocument);
      this.astCache = new Map();
    }

    /**
     * Saves selector and it's ast to cache.
     *
     * @param selector Standard or extended selector.
     * @param ast Selector ast.
     */
    return _createClass(ExtCssDocument, [{
      key: "saveAstToCache",
      value: function saveAstToCache(selector, ast) {
        this.astCache.set(selector, ast);
      }

      /**
       * Returns ast from cache for given selector.
       *
       * @param selector Standard or extended selector.
       *
       * @returns Previously parsed ast found in cache, or null if not found.
       */
    }, {
      key: "getAstFromCache",
      value: function getAstFromCache(selector) {
        var cachedAst = this.astCache.get(selector) || null;
        return cachedAst;
      }

      /**
       * Returns selector ast:
       * - if cached ast exists — returns it;
       * - if no cached ast — saves newly parsed ast to cache and returns it.
       *
       * @param selector Standard or extended selector.
       *
       * @returns Ast for `selector`.
       */
    }, {
      key: "getSelectorAst",
      value: function getSelectorAst(selector) {
        var ast = this.getAstFromCache(selector);
        if (!ast) {
          ast = parse(selector);
        }
        this.saveAstToCache(selector, ast);
        return ast;
      }

      /**
       * Selects elements by selector.
       *
       * @param selector Standard or extended selector.
       *
       * @returns Array of DOM elements.
       */
    }, {
      key: "querySelectorAll",
      value: function querySelectorAll(selector) {
        var ast = this.getSelectorAst(selector);
        return selectElementsByAst(ast);
      }
    }]);
  }();
  var extCssDocument = new ExtCssDocument();
  function _arrayWithHoles(r) {
    if (Array.isArray(r)) return r;
  }
  function _iterableToArrayLimit(r, l) {
    var t = null == r ? null : "undefined" != typeof Symbol && r[Symbol.iterator] || r["@@iterator"];
    if (null != t) {
      var e,
        n,
        i,
        u,
        a = [],
        f = !0,
        o = !1;
      try {
        if (i = (t = t.call(r)).next, 0 === l) {
          if (Object(t) !== t) return;
          f = !1;
        } else for (; !(f = (e = i.call(t)).done) && (a.push(e.value), a.length !== l); f = !0);
      } catch (r) {
        o = !0, n = r;
      } finally {
        try {
          if (!f && null != t["return"] && (u = t["return"](), Object(u) !== u)) return;
        } finally {
          if (o) throw n;
        }
      }
      return a;
    }
  }
  function _arrayLikeToArray(r, a) {
    (null == a || a > r.length) && (a = r.length);
    for (var e = 0, n = Array(a); e < a; e++) n[e] = r[e];
    return n;
  }
  function _unsupportedIterableToArray(r, a) {
    if (r) {
      if ("string" == typeof r) return _arrayLikeToArray(r, a);
      var t = {}.toString.call(r).slice(8, -1);
      return "Object" === t && r.constructor && (t = r.constructor.name), "Map" === t || "Set" === t ? Array.from(r) : "Arguments" === t || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(t) ? _arrayLikeToArray(r, a) : void 0;
    }
  }
  function _nonIterableRest() {
    throw new TypeError("Invalid attempt to destructure non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.");
  }
  function _slicedToArray(r, e) {
    return _arrayWithHoles(r) || _iterableToArrayLimit(r, e) || _unsupportedIterableToArray(r, e) || _nonIterableRest();
  }

  /**
   * Converts array of `entries` to object.
   * Object.fromEntries() polyfill because it is not supported by old browsers, e.g. Chrome 55.
   * Only first two elements of `entries` array matter, other will be skipped silently.
   *
   * @see {@link https://caniuse.com/?search=Object.fromEntries}
   *
   * @param entries Array of pairs.
   *
   * @returns Object converted from `entries`.
   */
  var getObjectFromEntries = function getObjectFromEntries(entries) {
    var object = {};
    entries.forEach(function (el) {
      var _el = _slicedToArray(el, 2),
        key = _el[0],
        value = _el[1];
      object[key] = value;
    });
    return object;
  };
  var DEBUG_PSEUDO_PROPERTY_KEY = 'debug';

  /**
   * Info about an action pseudo-class found in a selector.
   */

  /**
   * Finds top-level action pseudo-classes in the raw selector.
   *
   * Action pseudo-classes nested in other pseudo-classes are ignored here
   * and rejected later by the selector parser.
   *
   * @param rawSelector Raw selector text.
   *
   * @returns Found action pseudo-classes in the order of appearance.
   */
  var findActionSelectors = function findActionSelectors(rawSelector) {
    var found = [];
    scanSelector(rawSelector, {
      onTopLevelColon: function onTopLevelColon(index) {
        var marker = ACTION_PSEUDO_MARKERS.find(function (name) {
          var markerText = "".concat(name).concat(BRACKET.PARENTHESES.LEFT);
          var candidate = rawSelector.substring(index + 1, index + 1 + markerText.length);
          return candidate.toLowerCase() === markerText;
        });
        if (!marker) {
          return;
        }
        var argStartIndex = index + marker.length + 2;
        var closingIndex = findClosingParenthesis(rawSelector, argStartIndex, marker);
        found.push({
          marker: marker,
          startIndex: index,
          arg: closingIndex === -1 ? rawSelector.substring(argStartIndex) : rawSelector.substring(argStartIndex, closingIndex),
          endIndex: closingIndex === -1 ? -1 : closingIndex + 1
        });
        if (closingIndex !== -1) {
          return closingIndex;
        }
      }
    });
    return found;
  };

  /**
   * Checks the presence of action pseudo-classes and validates them
   * while parsing the selector part of a CSS rule.
   *
   * Action pseudo-classes are not about element selecting but about actions
   * with elements, so they are stripped from the selector and returned
   * as synthetic style declarations.
   *
   * @param rawSelector Selector which may contain an action pseudo-class.
   *
   * @returns Parsed selector data with selector and styles.
   * @throws An error on invalid action pseudo-class usage.
   */
  var parseActionSelector = function parseActionSelector(rawSelector) {
    var actions = findActionSelectors(rawSelector);
    if (actions.length === 0) {
      return {
        selector: rawSelector,
        stylesOfSelector: []
      };
    }
    var action = actions[0];
    if (actions.length > 1) {
      throwActionError(action.marker, 'MULTIPLE_USAGE', rawSelector);
    }
    if (action.endIndex === -1) {
      // an unclosed regexp literal is an argument error,
      // while any other unclosed argument is an action error
      var messageKey = action.arg.trim().startsWith(SLASH) ? 'INVALID_ARG' : 'INVALID_ACTION';
      throwActionError(action.marker, messageKey, rawSelector);
    }
    if (action.startIndex === 0) {
      throwActionError(action.marker, 'NO_TARGET_SELECTOR', rawSelector);
    }
    if (action.endIndex < rawSelector.length) {
      throwActionError(action.marker, 'INVALID_POSITION', rawSelector);
    }
    var selector = rawSelector.substring(0, action.startIndex);
    if (action.marker === REMOVE_PSEUDO_MARKER) {
      if (action.arg !== '') {
        throwActionError(action.marker, 'INVALID_ARG', rawSelector);
      }
      return {
        selector: selector,
        stylesOfSelector: [{
          property: REMOVE_PSEUDO_MARKER,
          value: PSEUDO_PROPERTY_POSITIVE_VALUE
        }]
      };
    }
    var arg = normalizeActionArg(action.arg);
    if (!arg) {
      throwActionError(action.marker, 'INVALID_ARG', rawSelector);
    }
    return {
      selector: selector,
      stylesOfSelector: [{
        property: action.marker,
        value: arg
      }]
    };
  };

  /**
   * Parses cropped selector part found before `{`.
   *
   * @param selectorBuffer Buffered selector to parse.
   * @param extCssDoc Needed for caching of selector ast.
   *
   * @returns Parsed validation data for cropped part of stylesheet which may be a selector.
   * @throws An error on unsupported CSS features, e.g. at-rules.
   */
  var parseSelectorRulePart = function parseSelectorRulePart(selectorBuffer, extCssDoc) {
    var selector = selectorBuffer.trim();
    if (selector.startsWith(AT_RULE_MARKER)) {
      throw new Error("".concat(NO_AT_RULE_ERROR_PREFIX, ": '").concat(selector, "'."));
    }
    var actionSelectorData;
    try {
      actionSelectorData = parseActionSelector(selector);
    } catch (e) {
      logger.error(getErrorMessage(e));
      throw e;
    }
    var stylesOfSelector = [];
    var success = false;
    var ast;
    try {
      selector = actionSelectorData.selector;
      stylesOfSelector = actionSelectorData.stylesOfSelector;
      // validate found selector by parsing it to ast
      // so if it is invalid error will be thrown
      ast = extCssDoc.getSelectorAst(selector);
      success = true;
    } catch (e) {
      success = false;
    }
    return {
      success: success,
      selector: selector,
      ast: ast,
      stylesOfSelector: stylesOfSelector
    };
  };

  /**
   * Creates a map for storing raw results of css rules parsing.
   * Used for merging styles for same selector.
   *
   * @returns Map where **key** is `selector`
   * and **value** is object with `ast` and `styles`.
   */
  var createRawResultsMap = function createRawResultsMap() {
    return new Map();
  };

  /**
   * Saves rules data for unique selectors.
   *
   * @param rawResults Previously collected results of parsing.
   * @param rawRuleData Parsed rule data.
   *
   * @throws An error if there is no rawRuleData.styles or rawRuleData.ast.
   */
  var saveToRawResults = function saveToRawResults(rawResults, rawRuleData) {
    var selector = rawRuleData.selector,
      ast = rawRuleData.ast,
      rawStyles = rawRuleData.rawStyles,
      hasActionPseudoClass = rawRuleData.hasActionPseudoClass;
    if (!rawStyles) {
      throw new Error("No style declaration for selector: '".concat(selector, "'"));
    }
    if (!ast) {
      throw new Error("No ast parsed for selector: '".concat(selector, "'"));
    }
    var styleGroup = {
      styles: rawStyles,
      hasActionPseudoClass: !!hasActionPseudoClass
    };
    var storedRuleData = rawResults.get(selector);
    if (!storedRuleData) {
      rawResults.set(selector, {
        ast: ast,
        styleGroups: [styleGroup]
      });
    } else {
      storedRuleData.styleGroups.push(styleGroup);
    }
  };

  /**
   * Checks whether the 'remove' property positively set in styles
   * with only one positive value - 'true'.
   *
   * @param styles Array of styles.
   *
   * @returns True if there is 'remove' property with 'true' value in `styles`.
   */
  var isRemoveSetInStyles = function isRemoveSetInStyles(styles) {
    return styles.some(function (s) {
      return s.property === REMOVE_PSEUDO_MARKER && s.value === PSEUDO_PROPERTY_POSITIVE_VALUE;
    });
  };

  /**
   * Checks whether styles contain action pseudo-class declarations.
   *
   * @param styles Array of styles.
   *
   * @returns True if styles contain action pseudo-class declarations.
   */
  var hasActionPseudoStyles = function hasActionPseudoStyles(styles) {
    return styles.some(function (s) {
      return isActionPseudoMarker(s.property);
    });
  };

  /**
   * Filters styles declared together with an action pseudo-class:
   * such styles are ignored except `content` and `debug` needed for internal usage.
   *
   * @param styles Array of styles.
   *
   * @returns Styles which should be kept.
   */
  var filterActionRuleStyles = function filterActionRuleStyles(styles) {
    return styles.filter(function (s) {
      return s.property === CONTENT_CSS_PROPERTY || s.property === DEBUG_PSEUDO_PROPERTY_KEY;
    });
  };

  /**
   * Removes action pseudo-class declarations from style block styles:
   * `remove-attr` and `remove-class` are supported only as pseudo-classes.
   *
   * @param styles Array of styles.
   *
   * @returns Styles without action pseudo-class declarations.
   */
  var filterStyleBlockActionDeclarations = function filterStyleBlockActionDeclarations(styles) {
    return styles.filter(function (s) {
      return !isActionStyleMarker(s.property);
    });
  };

  /**
   * Filters style-block declarations of a rule according to its selector:
   * if the selector contains an action pseudo-class, styles declared in the block
   * are ignored except `content` and `debug`;
   * otherwise action declarations (`remove-attr` and `remove-class`)
   * are removed as they are supported only as pseudo-classes.
   *
   * @param styles Parsed style-block declarations.
   * @param hasActionStyles True if the selector contains an action pseudo-class.
   *
   * @returns Styles which should be kept for the rule.
   */
  var filterStyleBlockDeclarations = function filterStyleBlockDeclarations(styles, hasActionStyles) {
    return hasActionStyles ? filterActionRuleStyles(styles) : filterStyleBlockActionDeclarations(styles);
  };

  /**
   * Returns 'debug' property value which is set in styles.
   *
   * @param styles Array of styles.
   *
   * @returns Value of 'debug' property if it is set in `styles`,
   * or `undefined` if the property is not found.
   */
  var getDebugStyleValue = function getDebugStyleValue(styles) {
    var debugStyle = styles.find(function (s) {
      return s.property === DEBUG_PSEUDO_PROPERTY_KEY;
    });
    return debugStyle === null || debugStyle === void 0 ? void 0 : debugStyle.value;
  };

  /**
   * Data of rules with the same selector needed to prepare the final rule data.
   */

  /**
   * Prepares final RuleData.
   * Handles `debug` and action pseudo-classes in raw rule data styles:
   * action declarations are moved to `actionStyles`,
   * regular styles of rules with the same selector are kept in `style`,
   * and `content` of action rules is kept in `style` only for `beforeStyleApplied`.
   *
   * @param rawRuleData Raw data of selector css rule parsing.
   *
   * @returns Parsed ExtendedCss rule data.
   * @throws An error if rawRuleData.ast or rawRuleData.styleGroups not defined.
   */
  var prepareRuleData = function prepareRuleData(rawRuleData) {
    var selector = rawRuleData.selector,
      ast = rawRuleData.ast,
      styleGroups = rawRuleData.styleGroups;
    if (!ast) {
      throw new Error("AST should be parsed for selector: '".concat(selector, "'"));
    }
    if (!styleGroups) {
      throw new Error("Styles should be parsed for selector: '".concat(selector, "'"));
    }
    var ruleData = {
      selector: selector,
      ast: ast
    };

    // split styles by their source:
    // action pseudo-class declarations, styles of rules without action pseudo-classes,
    // and internal styles of action rules (`content` and `debug` only)
    var actionStyles = [];
    var regularStyles = [];
    var actionRuleStyles = [];
    var allRawStyles = [];
    styleGroups.forEach(function (group) {
      allRawStyles.push.apply(allRawStyles, _toConsumableArray(group.styles));
      group.styles.forEach(function (style) {
        if (style.property === DEBUG_PSEUDO_PROPERTY_KEY) {
          // `debug` is handled as a separate property
          return;
        }
        if (group.hasActionPseudoClass) {
          if (isActionStyleMarker(style.property)) {
            actionStyles.push(style);
          } else if (!isActionPseudoMarker(style.property)) {
            actionRuleStyles.push(style);
          }
          // the 'remove' pseudo-class declaration is handled via `shouldRemove`
        } else {
          regularStyles.push(style);
        }
      });
    });
    var debugValue = getDebugStyleValue(allRawStyles);
    // set `debug` as separate property only if its value is valid
    // which is 'true' or 'global'
    if (debugValue === PSEUDO_PROPERTY_POSITIVE_VALUE || debugValue === DEBUG_PSEUDO_PROPERTY_GLOBAL_VALUE) {
      ruleData.debug = debugValue;
    }
    var shouldRemove = isRemoveSetInStyles(allRawStyles);
    if (shouldRemove || actionStyles.length > 0) {
      var actionStyleMap = {};
      if (shouldRemove) {
        // the element is removed, so only 'content' is needed for internal usage
        var contentStyle = [].concat(regularStyles, actionRuleStyles).find(function (s) {
          return s.property === CONTENT_CSS_PROPERTY;
        });
        if (contentStyle) {
          actionStyleMap[CONTENT_CSS_PROPERTY] = contentStyle.value;
          ruleData.hasInternalContent = true;
        }
        actionStyleMap[REMOVE_PSEUDO_MARKER] = PSEUDO_PROPERTY_POSITIVE_VALUE;
      } else {
        // keep styles of other rules with the same target selector
        regularStyles.forEach(function (style) {
          actionStyleMap[style.property] = style.value;
        });
        // keep 'content' of action rules only for `beforeStyleApplied` (e.g. CssHitsCounter);
        // regular 'content' has priority and should be applied to the element
        if (!(CONTENT_CSS_PROPERTY in actionStyleMap)) {
          var _contentStyle = actionRuleStyles.find(function (s) {
            return s.property === CONTENT_CSS_PROPERTY;
          });
          if (_contentStyle) {
            actionStyleMap[CONTENT_CSS_PROPERTY] = _contentStyle.value;
            ruleData.hasInternalContent = true;
          }
        }
      }

      // action pseudo-classes are not stored in `style`:
      // `actionStyles` is the only source of action arguments
      if (actionStyles.length > 0) {
        ruleData.actionStyles = actionStyles;
      }
      ruleData.style = actionStyleMap;
    } else {
      // otherwise all styles should be applied.
      // every style property will be unique because of their converting into object
      if (regularStyles.length > 0) {
        var stylesAsEntries = regularStyles.map(function (style) {
          var property = style.property,
            value = style.value;
          return [property, value];
        });
        var preparedStyleData = getObjectFromEntries(stylesAsEntries);
        ruleData.style = preparedStyleData;
      }
    }
    return ruleData;
  };

  /**
   * Combines previously parsed css rules data objects
   * into rules which are ready to apply.
   *
   * @param rawResults Previously parsed css rules data objects.
   *
   * @returns Parsed ExtendedCss rule data.
   */
  var combineRulesData = function combineRulesData(rawResults) {
    var results = [];
    rawResults.forEach(function (value, key) {
      var selector = key;
      var ast = value.ast,
        styleGroups = value.styleGroups;
      results.push(prepareRuleData({
        selector: selector,
        ast: ast,
        styleGroups: styleGroups
      }));
    });
    return results;
  };

  /**
   * Trims `rawStyle` and splits it into tokens.
   *
   * @param rawStyle Style declaration block content inside curly bracket — `{` and `}` —
   * can be a single style declaration or a list of declarations.
   *
   * @returns Array of tokens supported for style declaration block.
   */
  var tokenizeStyleBlock = function tokenizeStyleBlock(rawStyle) {
    var styleDeclaration = rawStyle.trim();
    return tokenize(styleDeclaration, SUPPORTED_STYLE_DECLARATION_MARKS);
  };

  /**
   * Describes possible style declaration parts.
   *
   * IMPORTANT: it is used as 'const' instead of 'enum' to avoid side effects
   * during ExtendedCss import into other libraries.
   */
  var DECLARATION_PART = {
    PROPERTY: 'property',
    VALUE: 'value'
  };

  /**
   * Interface for style declaration parser context.
   */

  /**
   * Checks whether the quotes has been opened for style value.
   *
   * @param context Style block parser context.
   *
   * @returns True if style value has already opened quotes.
   */
  var isValueQuotesOpen = function isValueQuotesOpen(context) {
    return context.bufferValue !== '' && context.valueQuoteMark !== null;
  };

  /**
   * Saves parsed property and value to collection of parsed styles.
   * Prunes context buffers for property and value.
   *
   * @param context Style block parser context.
   */
  var collectStyle = function collectStyle(context) {
    context.styles.push({
      property: context.bufferProperty.trim(),
      value: context.bufferValue.trim()
    });
    // reset buffers
    context.bufferProperty = '';
    context.bufferValue = '';
  };

  /**
   * Handles token which is supposed to be a part of style **property**.
   *
   * @param context Style block parser context.
   * @param styleBlock Whole style block which is being parsed.
   * @param token Current token.
   *
   * @throws An error on invalid token.
   */
  var processPropertyToken = function processPropertyToken(context, styleBlock, token) {
    var tokenValue = token.value;
    switch (token.type) {
      case TOKEN_TYPE.WORD:
        if (context.bufferProperty.length > 0) {
          // e.g. 'padding top: 0;' - current tokenValue is 'top' which is not valid
          throw new Error("Invalid style property in style block: '".concat(styleBlock, "'"));
        }
        context.bufferProperty += tokenValue;
        break;
      case TOKEN_TYPE.MARK:
        // only colon and whitespaces are allowed while style property parsing
        if (tokenValue === COLON) {
          if (context.bufferProperty.trim().length === 0) {
            // e.g. such style block: '{ : none; }'
            throw new Error("Missing style property before ':' in style block: '".concat(styleBlock, "'"));
          }
          // the property successfully collected
          context.bufferProperty = context.bufferProperty.trim();
          // prepare for value collecting
          context.processing = DECLARATION_PART.VALUE;
          // the property buffer shall be reset after the value is successfully collected
        } else if (WHITE_SPACE_CHARACTERS.includes(tokenValue)) ;else {
          // if after the property there is anything other than ':' except whitespace, this is a parse error
          // https://www.w3.org/TR/css-syntax-3/#consume-declaration
          throw new Error("Invalid style declaration in style block: '".concat(styleBlock, "'"));
        }
        break;
      default:
        throw new Error("Unsupported style property character: '".concat(tokenValue, "' in style block: '").concat(styleBlock, "'"));
    }
  };

  /**
   * Handles token which is supposed to be a part of style **value**.
   *
   * @param context Style block parser context.
   * @param styleBlock Whole style block which is being parsed.
   * @param token Current token.
   *
   * @throws An error on invalid token.
   */
  var processValueToken = function processValueToken(context, styleBlock, token) {
    var tokenValue = token.value;
    if (token.type === TOKEN_TYPE.WORD) {
      // simply collect to buffer
      context.bufferValue += tokenValue;
    } else {
      // otherwise check the mark
      switch (tokenValue) {
        case COLON:
          // the ':' character inside of the value should be inside of quotes
          // otherwise the value is not valid
          // e.g. 'content: display: none'
          // parser is here        ↑
          if (!isValueQuotesOpen(context)) {
            // eslint-disable-next-line max-len
            throw new Error("Invalid style value for property '".concat(context.bufferProperty, "' in style block: '").concat(styleBlock, "'"));
          }
          // collect the colon inside quotes
          // e.g. 'content: "test:123"'
          // parser is here      ↑
          context.bufferValue += tokenValue;
          break;
        case SEMICOLON:
          if (isValueQuotesOpen(context)) {
            // ';' inside quotes is part of style value
            // e.g. 'content: "test;"'
            context.bufferValue += tokenValue;
          } else {
            // otherwise the value is successfully collected
            // save parsed style
            collectStyle(context);
            // prepare for value collecting
            context.processing = DECLARATION_PART.PROPERTY;
          }
          break;
        case SINGLE_QUOTE:
        case DOUBLE_QUOTE:
          // if quotes are not open
          if (context.valueQuoteMark === null) {
            // save the opening quote mark for later comparison
            context.valueQuoteMark = tokenValue;
          } else if (!context.bufferValue.endsWith(BACKSLASH)
          // otherwise a quote appeared in the value earlier,
          // and non-escaped quote should be checked whether it is a closing quote
          && context.valueQuoteMark === tokenValue) {
            context.valueQuoteMark = null;
          }
          // always save the quote to the buffer
          // but after the context.bufferValue is checked for BACKSLASH above
          // e.g. 'content: "test:123"'
          //      'content: "\""'
          context.bufferValue += tokenValue;
          break;
        case BACKSLASH:
          if (!isValueQuotesOpen(context)) {
            // eslint-disable-next-line max-len
            throw new Error("Invalid style value for property '".concat(context.bufferProperty, "' in style block: '").concat(styleBlock, "'"));
          }
          // collect the backslash inside quotes
          // e.g. ' content: "\"" '
          // parser is here   ↑
          context.bufferValue += tokenValue;
          break;
        case SPACE:
        case TAB:
        case CARRIAGE_RETURN:
        case LINE_FEED:
        case FORM_FEED:
          // whitespace should be collected only if the value collecting started
          // which means inside of the value
          // e.g. 'width: 100% !important'
          // parser is here   ↑
          if (context.bufferValue.length > 0) {
            context.bufferValue += tokenValue;
          }
          // otherwise it can be omitted
          // e.g. 'width:  100% !important'
          // here        ↑
          break;
        default:
          throw new Error("Unknown style declaration token: '".concat(tokenValue, "'"));
      }
    }
  };

  /**
   * Parses css rule style block.
   *
   * @param rawStyleBlock Style block to parse.
   *
   * @returns Array of style declarations.
   * @throws An error on invalid style block.
   */
  var parseStyleBlock = function parseStyleBlock(rawStyleBlock) {
    var styleBlock = rawStyleBlock.trim();
    var tokens = tokenizeStyleBlock(styleBlock);
    var context = {
      // style declaration parsing always starts with 'property'
      processing: DECLARATION_PART.PROPERTY,
      styles: [],
      bufferProperty: '',
      bufferValue: '',
      valueQuoteMark: null
    };
    var i = 0;
    while (i < tokens.length) {
      var token = tokens[i];
      if (!token) {
        break;
      }
      if (context.processing === DECLARATION_PART.PROPERTY) {
        processPropertyToken(context, styleBlock, token);
      } else if (context.processing === DECLARATION_PART.VALUE) {
        processValueToken(context, styleBlock, token);
      } else {
        throw new Error('Style declaration parsing failed');
      }
      i += 1;
    }

    // unbalanced value quotes
    // e.g. 'content: "test} '
    if (isValueQuotesOpen(context)) {
      throw new Error("Unbalanced style declaration quotes in style block: '".concat(styleBlock, "'"));
    }

    // collected property and value have not been saved to styles;
    // it is possible for style block with no semicolon at the end
    // e.g. such style block: '{ display: none }'
    if (context.bufferProperty.length > 0) {
      if (context.bufferValue.length === 0) {
        // e.g. such style blocks:
        //   '{ display:  }'
        //   '{ remove }'
        // eslint-disable-next-line max-len
        throw new Error("Missing style value for property '".concat(context.bufferProperty, "' in style block '").concat(styleBlock, "'"));
      }
      collectStyle(context);
    }

    // rule with empty style block
    // e.g. 'div { }'
    if (context.styles.length === 0) {
      throw new Error(STYLE_ERROR_PREFIX.NO_STYLE);
    }
    return context.styles;
  };

  // TODO: use `extCssDoc` for caching of style block parser results
  /**
   * Parses CSS rule into rules data object:
   * 1. Find the last `{` mark in the rule
   *    which supposed to be a divider between selector and style block.
   * 2. Validates found string part before the `{` via selector parser; and if:
   *  - parsing failed – get the previous `{` in the rule,
   *    and validates a new rule part again [2];
   *  - parsing successful — saves a found rule part as selector and parses the style block.
   *
   * @param rawCssRule Single CSS rule to parse.
   * @param extCssDoc ExtCssDocument which is used for selector ast caching.
   *
   * @returns Array of rules data which contains:
   *   - selector as string;
   *   - ast to query elements by;
   *   - map of styles to apply.
   * @throws An error on invalid css rule syntax:
   *   - unsupported CSS features – comments and at-rules
   *   - invalid selector or style block.
   */
  var parseRule = function parseRule(rawCssRule, extCssDoc) {
    var _rawRuleData$selector;
    var cssRule = rawCssRule.trim();
    if (cssRule.includes("".concat(SLASH).concat(ASTERISK)) && cssRule.includes("".concat(ASTERISK).concat(SLASH))) {
      throw new Error(STYLE_ERROR_PREFIX.NO_COMMENT);
    }

    // braces inside quoted strings, regexp literals and action arguments are skipped
    var leftCurlyBracketIndexes = findStyleBlockStarts(cssRule);

    // rule with style block but no selector
    // e.g. '{ display: none; }'
    if (getFirst(leftCurlyBracketIndexes) === 0) {
      throw new Error(NO_SELECTOR_ERROR_PREFIX);
    }
    var selectorData;

    // if rule has `{` but there is no `}`
    if (leftCurlyBracketIndexes.length > 0 && !cssRule.includes(BRACKET.CURLY.RIGHT)) {
      throw new Error("".concat(STYLE_ERROR_PREFIX.NO_STYLE, " OR ").concat(STYLE_ERROR_PREFIX.UNCLOSED_STYLE));
    }
    if (
    // if rule has no `{`
    leftCurlyBracketIndexes.length === 0
    // or `}`
    || !cssRule.includes(BRACKET.CURLY.RIGHT)) {
      try {
        // the whole css rule considered as "selector part"
        // which may contain an action pseudo-class
        selectorData = parseSelectorRulePart(cssRule, extCssDoc);
        if (selectorData.success) {
          var _selectorData$stylesO;
          // rule with no style block has a valid action pseudo-class
          // which is parsed into "styles"
          // e.g. 'div:remove()'
          // but also it can be just selector with no styles
          // e.g. 'div'
          // which should not be considered as valid css rule
          if (((_selectorData$stylesO = selectorData.stylesOfSelector) === null || _selectorData$stylesO === void 0 ? void 0 : _selectorData$stylesO.length) === 0) {
            throw new Error(STYLE_ERROR_PREFIX.NO_STYLE_OR_ACTION);
          }
          return {
            selector: selectorData.selector.trim(),
            ast: selectorData.ast,
            rawStyles: selectorData.stylesOfSelector,
            hasActionPseudoClass: true
          };
        } else {
          // not valid selector
          throw new Error('Invalid selector');
        }
      } catch (e) {
        throw new Error(getErrorMessage(e));
      }
    }
    var selectorBuffer;
    var styleBlockBuffer;
    var rawRuleData = {
      selector: ''
    };

    // css rule should be parsed from its end
    for (var i = leftCurlyBracketIndexes.length - 1; i > -1; i -= 1) {
      var index = leftCurlyBracketIndexes[i];
      if (!index) {
        throw new Error("Impossible to continue, no '{' to process for rule: '".concat(cssRule, "'"));
      }
      // selector is before `{`, style block is after it
      selectorBuffer = cssRule.slice(0, index);
      // skip curly brackets
      styleBlockBuffer = cssRule.slice(index + 1, cssRule.length - 1);
      selectorData = parseSelectorRulePart(selectorBuffer, extCssDoc);
      if (selectorData.success) {
        var _rawRuleData$rawStyle2, _rawRuleData$rawStyle3;
        var _rawRuleData$rawStyle;
        // selector successfully parsed
        rawRuleData.selector = selectorData.selector.trim();
        rawRuleData.ast = selectorData.ast;
        rawRuleData.rawStyles = selectorData.stylesOfSelector;
        // style block should be parsed
        // TODO: add cache for style block parsing
        var parsedStyles = parseStyleBlock(styleBlockBuffer);
        var hasActionStyles = hasActionPseudoStyles((_rawRuleData$rawStyle2 = rawRuleData.rawStyles) !== null && _rawRuleData$rawStyle2 !== void 0 ? _rawRuleData$rawStyle2 : []);
        rawRuleData.hasActionPseudoClass = hasActionStyles;
        var stylesToPush = filterStyleBlockDeclarations(parsedStyles, hasActionStyles);
        if (stylesToPush.length === 0 && !hasActionStyles) {
          // rule with only ignored action declarations has nothing to apply
          throw new Error("".concat(STYLE_ERROR_PREFIX.NO_STYLE, ": '").concat(cssRule, "'"));
        }
        (_rawRuleData$rawStyle = rawRuleData.rawStyles) === null || _rawRuleData$rawStyle === void 0 || (_rawRuleData$rawStyle3 = _rawRuleData$rawStyle).push.apply(_rawRuleData$rawStyle3, _toConsumableArray(stylesToPush));
        // stop rule parsing
        break;
      } else {
        // if selector was not parsed successfully
        // continue with next index of `{`
        continue;
      }
    }
    if (((_rawRuleData$selector = rawRuleData.selector) === null || _rawRuleData$selector === void 0 ? void 0 : _rawRuleData$selector.length) === 0) {
      // skip the rule as selector
      throw new Error('Selector in not valid');
    }
    return rawRuleData;
  };

  /**
   * Parses array of CSS rules into array of rules data objects.
   * Invalid rules are skipped and not applied,
   * and the errors are logged.
   *
   * @param rawCssRules Array of rules to parse.
   * @param extCssDoc Needed for selector ast caching.
   *
   * @returns Array of parsed valid rules data.
   */
  var parseRules = function parseRules(rawCssRules, extCssDoc) {
    var rawResults = createRawResultsMap();
    var warnings = [];
    // trim all rules and find unique ones
    var uniqueRules = _toConsumableArray(new Set(rawCssRules.map(function (r) {
      return r.trim();
    })));
    uniqueRules.forEach(function (rule) {
      try {
        saveToRawResults(rawResults, parseRule(rule, extCssDoc));
      } catch (e) {
        // skip the invalid rule
        var errorMessage = getErrorMessage(e);
        warnings.push("'".concat(rule, "' - error: '").concat(errorMessage, "'"));
      }
    });

    // log info about skipped invalid rules
    if (warnings.length > 0) {
      logger.info("Invalid rules:\n  ".concat(warnings.join('\n  ')));
    }
    return combineRulesData(rawResults);
  };
  var REGEXP_DECLARATION_END = /[;}]/g;
  var REGEXP_DECLARATION_DIVIDER = /[;:}]/g;
  var REGEXP_NON_WHITESPACE = /\S/g;

  /**
   * Interface for stylesheet parser context.
   */

  /**
   * Resets rule data buffer to init value after rule successfully collected.
   *
   * @param context Stylesheet parser context.
   */
  var restoreRuleAcc = function restoreRuleAcc(context) {
    context.rawRuleData = {
      selector: ''
    };
  };

  /**
   * Parses cropped selector part found before `{` previously.
   *
   * @param context Stylesheet parser context.
   * @param extCssDoc Needed for caching of selector ast.
   *
   * @returns Parsed validation data for cropped part of stylesheet which may be a selector.
   * @throws An error on unsupported CSS features, e.g. at-rules.
   */
  var parseSelectorPart = function parseSelectorPart(context, extCssDoc) {
    var selector = context.selectorBuffer.trim();
    if (selector.startsWith(AT_RULE_MARKER)) {
      throw new Error("".concat(NO_AT_RULE_ERROR_PREFIX, ": '").concat(selector, "'."));
    }
    var actionSelectorData;
    try {
      actionSelectorData = parseActionSelector(selector);
    } catch (e) {
      logger.error(getErrorMessage(e));
      throw e;
    }
    if (context.nextIndex === -1) {
      if (selector === actionSelectorData.selector) {
        // rule should have style or action pseudo-class
        throw new Error("".concat(STYLE_ERROR_PREFIX.NO_STYLE_OR_ACTION, ": '").concat(context.cssToParse, "'"));
      }
      // stop parsing as there is no style declaration and selector parsed fine
      context.cssToParse = '';
    }
    var stylesOfSelector = [];
    var success = false;
    var ast;
    try {
      selector = actionSelectorData.selector;
      stylesOfSelector = actionSelectorData.stylesOfSelector;
      // validate found selector by parsing it to ast
      // so if it is invalid error will be thrown
      ast = extCssDoc.getSelectorAst(selector);
      success = true;
    } catch (e) {
      success = false;
    }
    if (context.nextIndex > 0) {
      // slice found valid selector part off
      // and parse rest of stylesheet later
      context.cssToParse = context.cssToParse.slice(context.nextIndex);
    }
    return {
      success: success,
      selector: selector,
      ast: ast,
      stylesOfSelector: stylesOfSelector
    };
  };

  /**
   * Recursively parses style declaration string into `Style`s.
   *
   * @param context Stylesheet parser context.
   * @param styles Array of styles.
   *
   * @throws An error on invalid style declaration.
   * @returns A number index of the next `}` in `this.cssToParse`.
   */
  var _parseUntilClosingBracket = function parseUntilClosingBracket(context, styles) {
    // Expects ":", ";", and "}".
    REGEXP_DECLARATION_DIVIDER.lastIndex = context.nextIndex;
    var match = REGEXP_DECLARATION_DIVIDER.exec(context.cssToParse);
    if (match === null) {
      throw new Error("".concat(STYLE_ERROR_PREFIX.INVALID_STYLE, ": '").concat(context.cssToParse, "'"));
    }
    var matchPos = match.index;
    var matched = match[0];
    if (matched === BRACKET.CURLY.RIGHT) {
      var declarationChunk = context.cssToParse.slice(context.nextIndex, matchPos);
      if (declarationChunk.trim().length === 0) {
        // empty style declaration
        // e.g. 'div { }'
        if (styles.length === 0) {
          throw new Error("".concat(STYLE_ERROR_PREFIX.NO_STYLE, ": '").concat(context.cssToParse, "'"));
        }
        // else valid style parsed before it
        // e.g. '{ display: none; }' -- position is after ';'
      } else {
        // closing curly bracket '}' is matched before colon ':'
        // trimmed declarationChunk is not a space, between ';' and '}',
        // e.g. 'visible }' in style '{ display: none; visible }' after part before ';' is parsed
        throw new Error("".concat(STYLE_ERROR_PREFIX.INVALID_STYLE, ": '").concat(context.cssToParse, "'"));
      }
      return matchPos;
    }
    if (matched === COLON) {
      var colonIndex = matchPos;
      // Expects ";" and "}".
      REGEXP_DECLARATION_END.lastIndex = colonIndex;
      match = REGEXP_DECLARATION_END.exec(context.cssToParse);
      if (match === null) {
        throw new Error("".concat(STYLE_ERROR_PREFIX.UNCLOSED_STYLE, ": '").concat(context.cssToParse, "'"));
      }
      matchPos = match.index;
      matched = match[0];
      // Populates the `styleMap` key-value map.
      var property = context.cssToParse.slice(context.nextIndex, colonIndex).trim();
      if (property.length === 0) {
        throw new Error("".concat(STYLE_ERROR_PREFIX.NO_PROPERTY, ": '").concat(context.cssToParse, "'"));
      }
      var value = context.cssToParse.slice(colonIndex + 1, matchPos).trim();
      if (value.length === 0) {
        throw new Error("".concat(STYLE_ERROR_PREFIX.NO_VALUE, ": '").concat(context.cssToParse, "'"));
      }
      styles.push({
        property: property,
        value: value
      });
      // finish style parsing if '}' is found
      // e.g. '{ display: none }' -- no ';' at the end of declaration
      if (matched === BRACKET.CURLY.RIGHT) {
        return matchPos;
      }
    }
    // matchPos is the position of the next ';'
    // crop 'cssToParse' and re-run the loop
    context.cssToParse = context.cssToParse.slice(matchPos + 1);
    context.nextIndex = 0;
    return _parseUntilClosingBracket(context, styles); // Should be a subject of tail-call optimization.
  };

  /**
   * Parses next style declaration part in stylesheet.
   *
   * @param context Stylesheet parser context.
   *
   * @returns Array of style data objects.
   */
  var parseNextStyle = function parseNextStyle(context) {
    var styles = [];
    var styleEndPos = _parseUntilClosingBracket(context, styles);

    // find next rule after the style declaration
    REGEXP_NON_WHITESPACE.lastIndex = styleEndPos + 1;
    var match = REGEXP_NON_WHITESPACE.exec(context.cssToParse);
    if (match === null) {
      context.cssToParse = '';
      return styles;
    }
    var matchPos = match.index;

    // cut out matched style declaration for previous selector
    context.cssToParse = context.cssToParse.slice(matchPos);
    return styles;
  };

  /**
   * Parses stylesheet of rules into rules data objects (non-recursively):
   * 1. Iterates through stylesheet string.
   * 2. Finds first `{` which can be style declaration start or part of selector.
   * 3. Validates found string part via selector parser; and if:
   *  - it throws error — saves string part to buffer as part of selector,
   *    slice next stylesheet part to `{` [2] and validates again [3];
   *  - no error — saves found string part as selector and starts to parse styles (recursively).
   *
   * @param rawStylesheet Raw stylesheet as string.
   * @param extCssDoc ExtCssDocument which uses cache while selectors parsing.
   * @throws An error on unsupported CSS features, e.g. comments or invalid stylesheet syntax.
   * @returns Array of rules data which contains:
   * - selector as string;
   * - ast to query elements by;
   * - map of styles to apply.
   */
  var parseStylesheet = function parseStylesheet(rawStylesheet, extCssDoc) {
    var stylesheet = rawStylesheet.trim();
    if (stylesheet.includes("".concat(SLASH).concat(ASTERISK)) && stylesheet.includes("".concat(ASTERISK).concat(SLASH))) {
      throw new Error("".concat(STYLE_ERROR_PREFIX.NO_COMMENT, " in stylesheet: '").concat(stylesheet, "'"));
    }
    var context = {
      // any stylesheet should start with selector
      isSelector: true,
      // init value of parser position
      nextIndex: 0,
      // init value of cssToParse
      cssToParse: stylesheet,
      // buffer for collecting selector part
      selectorBuffer: '',
      // accumulator for rules
      rawRuleData: {
        selector: ''
      }
    };
    var rawResults = createRawResultsMap();
    var selectorData;

    // context.cssToParse is going to be cropped while its parsing
    while (context.cssToParse) {
      if (context.isSelector) {
        // find index of first opening curly bracket
        // which may mean start of style part and end of selector one;
        // the scan stops at the first bracket to keep parsing linear
        context.nextIndex = findStyleBlockStart(context.cssToParse);
        // rule should not start with style, selector is required
        // e.g. '{ display: none; }'
        if (context.selectorBuffer.length === 0 && context.nextIndex === 0) {
          throw new Error("".concat(STYLE_ERROR_PREFIX.NO_SELECTOR, ": '").concat(context.cssToParse, "'"));
        }
        if (context.nextIndex === -1) {
          // no style declaration in rule
          // but rule still may contain action pseudo-class
          context.selectorBuffer = context.cssToParse;
        } else {
          // collect string parts before opening curly bracket
          // until valid selector collected
          context.selectorBuffer += context.cssToParse.slice(0, context.nextIndex);
        }
        selectorData = parseSelectorPart(context, extCssDoc);
        if (selectorData.success) {
          var _selectorData$stylesO2;
          // selector successfully parsed
          context.rawRuleData.selector = selectorData.selector.trim();
          context.rawRuleData.ast = selectorData.ast;
          context.rawRuleData.rawStyles = selectorData.stylesOfSelector;
          context.rawRuleData.hasActionPseudoClass = hasActionPseudoStyles((_selectorData$stylesO2 = selectorData.stylesOfSelector) !== null && _selectorData$stylesO2 !== void 0 ? _selectorData$stylesO2 : []);
          context.isSelector = false;
          // save rule data if there is no style declaration
          if (context.nextIndex === -1) {
            saveToRawResults(rawResults, context.rawRuleData);
            // clean up ruleContext
            restoreRuleAcc(context);
          } else {
            // skip the opening curly bracket at the start of style declaration part
            context.nextIndex = 1;
            context.selectorBuffer = '';
          }
        } else {
          // if selector was not successfully parsed parseSelectorPart(), continue stylesheet parsing:
          // save the found bracket to buffer and proceed to next loop iteration
          context.selectorBuffer += BRACKET.CURLY.LEFT;
          // delete `{` from cssToParse
          context.cssToParse = context.cssToParse.slice(1);
        }
      } else {
        // style declaration should be parsed
        var parsedStyles = parseNextStyle(context);

        // styles can be parsed from selector part if it has an action pseudo-class
        // e.g. '.banner:remove() { debug: true; }'
        var hasActionStyles = !!context.rawRuleData.hasActionPseudoClass;
        var stylesToPush = filterStyleBlockDeclarations(parsedStyles, hasActionStyles);

        // rule with only ignored action declarations has nothing to apply
        if (stylesToPush.length > 0 || hasActionStyles) {
          var _context$rawRuleData$2;
          var _context$rawRuleData$;
          (_context$rawRuleData$ = context.rawRuleData.rawStyles) === null || _context$rawRuleData$ === void 0 || (_context$rawRuleData$2 = _context$rawRuleData$).push.apply(_context$rawRuleData$2, _toConsumableArray(stylesToPush));
          // save rule data to results
          saveToRawResults(rawResults, context.rawRuleData);
        }
        context.nextIndex = 0;

        // clean up ruleContext
        restoreRuleAcc(context);

        // parse next rule selector after style successfully parsed
        context.isSelector = true;
      }
    }
    return combineRulesData(rawResults);
  };

  /**
   * Checks whether passed `arg` is number type.
   *
   * @param arg Value to check.
   *
   * @returns True if `arg` is number and not NaN.
   */
  var isNumber = function isNumber(arg) {
    return typeof arg === 'number' && !Number.isNaN(arg);
  };

  /**
   * The purpose of ThrottleWrapper is to throttle calls of the function
   * that applies ExtendedCss rules. The reasoning here is that the function calls
   * are triggered by MutationObserver and there may be many mutations in a short period of time.
   * We do not want to apply rules on every mutation so we use this helper to make sure
   * that there is only one call in the given amount of time.
   */
  var ThrottleWrapper = /*#__PURE__*/function () {
    /**
     * Creates new ThrottleWrapper.
     * The {@link callback} should be executed not more often than {@link ThrottleWrapper.THROTTLE_DELAY_MS}.
     *
     * @param callback The callback.
     */
    function ThrottleWrapper(callback) {
      _classCallCheck(this, ThrottleWrapper);
      this.callback = callback;
      this.executeCallback = this.executeCallback.bind(this);
    }

    /**
     * Calls the `this.callback` function and update bounded throttle wrapper properties.
     */
    return _createClass(ThrottleWrapper, [{
      key: "executeCallback",
      value: function executeCallback() {
        this.lastRunTime = performance.now();
        if (isNumber(this.timerId)) {
          clearTimeout(this.timerId);
          delete this.timerId;
        }
        this.callback();
      }

      /**
       * Schedules the {@link executeCallback} function execution via setTimeout.
       * It may triggered by MutationObserver job which may occur too ofter, so we limit the function execution:
       *
       * 1. If {@link timerId} is set, ignore the call, because the function is already scheduled to be executed;
       *
       * 2. If {@link lastRunTime} is set, we need to check the time elapsed time since the last call. If it is
       * less than {@link ThrottleWrapper.THROTTLE_DELAY_MS}, we schedule the function execution after the remaining time.
       *
       * Otherwise, we execute the function asynchronously to ensure that it is executed
       * in the correct order with respect to DOM events, by deferring its execution until after
       * those tasks have completed.
       */
    }, {
      key: "run",
      value: function run() {
        if (isNumber(this.timerId)) {
          // there is a pending execution scheduled
          return;
        }
        if (isNumber(this.lastRunTime)) {
          var elapsedTime = performance.now() - this.lastRunTime;
          if (elapsedTime < ThrottleWrapper.THROTTLE_DELAY_MS) {
            this.timerId = window.setTimeout(this.executeCallback, ThrottleWrapper.THROTTLE_DELAY_MS - elapsedTime);
            return;
          }
        }

        /**
         * We use `setTimeout` instead `requestAnimationFrame`
         * here because requestAnimationFrame can be delayed for a long time
         * when the browser saves battery or the engine is heavily loaded.
         */
        this.timerId = window.setTimeout(this.executeCallback);
      }
    }]);
  }();
  _defineProperty(ThrottleWrapper, "THROTTLE_DELAY_MS", 150);
  var LAST_EVENT_TIMEOUT_MS = 10;
  var IGNORED_EVENTS = ['mouseover', 'mouseleave', 'mouseenter', 'mouseout'];
  var SUPPORTED_EVENTS = [
  // keyboard events
  'keydown', 'keypress', 'keyup',
  // mouse events
  'auxclick', 'click', 'contextmenu', 'dblclick', 'mousedown', 'mouseenter', 'mouseleave', 'mousemove', 'mouseover', 'mouseout', 'mouseup', 'pointerlockchange', 'pointerlockerror', 'select', 'wheel'];

  // 'wheel' event makes scrolling in Safari twitchy
  // https://github.com/AdguardTeam/ExtendedCss/issues/120
  var SAFARI_PROBLEMATIC_EVENTS = ['wheel'];

  /**
   * We use EventTracker to track the event that is likely to cause the mutation.
   * The problem is that we cannot use `window.event` directly from the mutation observer call
   * as we're not in the event handler context anymore.
   */
  var EventTracker = /*#__PURE__*/function () {
    /**
     * Creates new EventTracker.
     */
    function EventTracker() {
      var _this4 = this;
      _classCallCheck(this, EventTracker);
      _defineProperty(this, "getLastEventType", function () {
        return _this4.lastEventType;
      });
      _defineProperty(this, "getTimeSinceLastEvent", function () {
        if (!_this4.lastEventTime) {
          return null;
        }
        return Date.now() - _this4.lastEventTime;
      });
      this.trackedEvents = isSafariBrowser ? SUPPORTED_EVENTS.filter(function (event) {
        return !SAFARI_PROBLEMATIC_EVENTS.includes(event);
      }) : SUPPORTED_EVENTS;
      this.trackedEvents.forEach(function (eventName) {
        document.documentElement.addEventListener(eventName, _this4.trackEvent, true);
      });
    }

    /**
     * Callback for event listener for events tracking.
     *
     * @param event Any event.
     */
    return _createClass(EventTracker, [{
      key: "trackEvent",
      value: function trackEvent(event) {
        this.lastEventType = event.type;
        this.lastEventTime = Date.now();
      }
      /**
       * Checks whether the last caught event should be ignored.
       *
       * @returns True if event should be ignored.
       */
    }, {
      key: "isIgnoredEventType",
      value: function isIgnoredEventType() {
        var lastEventType = this.getLastEventType();
        var sinceLastEventTime = this.getTimeSinceLastEvent();
        return !!lastEventType && IGNORED_EVENTS.includes(lastEventType) && !!sinceLastEventTime && sinceLastEventTime < LAST_EVENT_TIMEOUT_MS;
      }

      /**
       * Stops event tracking by removing event listener.
       */
    }, {
      key: "stopTracking",
      value: function stopTracking() {
        var _this5 = this;
        this.trackedEvents.forEach(function (eventName) {
          document.documentElement.removeEventListener(eventName, _this5.trackEvent, true);
        });
      }
    }]);
  }();
  /**
   * Matches a CSS attribute selector and captures the attribute name.
   */
  var ATTRIBUTE_SELECTOR_REGEXP = /\[\s*([a-zA-Z_][\w-]*)/g;

  /**
   * Matches an attribute reference inside an `:xpath()` argument, e.g. `@data-on`.
   */
  var XPATH_ATTRIBUTE_REGEXP = /@([\w-]+)/g;

  /**
   * Matches a `:matches-attr()` pseudo-class and captures its argument:
   * a regexp literal (which can contain parentheses) or a plain name pattern.
   */
  var MATCHES_ATTR_REGEXP = new RegExp(":".concat(MATCHES_ATTR_PSEUDO_CLASS_MARKER, "\\(\\s*(\\/(?:\\\\.|[^/\\\\])*\\/[a-z]*|[^)]+)\\)"), 'gi');

  /**
   * Data needed to observe DOM mutations which can affect applied rules.
   */

  /**
   * Collects captured values of a regexp into the names array.
   *
   * @param input Scanned string.
   * @param regexp Regexp with a capturing group.
   * @param names Array to push captured values to.
   */
  var collectMatches = function collectMatches(input, regexp, names) {
    regexp.lastIndex = 0;
    var match = regexp.exec(input);
    while (match !== null) {
      if (match[1]) {
        names.push(match[1]);
      }
      match = regexp.exec(input);
    }
  };

  /**
   * Removes surrounding quotes from a value if they are present.
   *
   * @param value Value to process.
   *
   * @returns Value without surrounding quotes.
   */
  var stripQuotes = function stripQuotes(value) {
    if (value.length > 1 && (value.startsWith(DOUBLE_QUOTE) && value.endsWith(DOUBLE_QUOTE) || value.startsWith(SINGLE_QUOTE) && value.endsWith(SINGLE_QUOTE))) {
      return value.slice(1, -1);
    }
    return value;
  };

  /**
   * Collects attribute names used in a selector, e.g. 'data-x' from '[data-x="1"]'.
   *
   * The extraction is textual and covers plain attribute names:
   * namespaced (e.g. `[xml|lang]`) or escaped names may be missed,
   * and attribute-like text inside quoted values may be collected as a name.
   * False positives only widen the observation, so they are harmless.
   *
   * Both the original and the lower-cased spelling are returned:
   * HTML attribute names are lower-cased in mutation records,
   * while SVG ones preserve their case.
   *
   * @param selector Selector text.
   *
   * @returns Attribute names found in the selector.
   */
  var getSelectorAttributeNames = function getSelectorAttributeNames(selector) {
    var names = [];
    // CSS attribute selectors
    collectMatches(selector, ATTRIBUTE_SELECTOR_REGEXP, names);
    // attribute references inside :xpath() arguments, e.g. '@data-on'
    collectMatches(selector, XPATH_ATTRIBUTE_REGEXP, names);
    var result = [];
    names.forEach(function (name) {
      result.push(name);
      var lowerCasedName = name.toLowerCase();
      if (lowerCasedName !== name) {
        result.push(lowerCasedName);
      }
    });
    return result;
  };

  /**
   * Collects `:matches-attr()` name arguments from a selector.
   *
   * @param selector Selector text.
   *
   * @returns Name arguments of `:matches-attr()` pseudo-classes.
   */
  var getMatchesAttrNames = function getMatchesAttrNames(selector) {
    var args = [];
    collectMatches(selector, MATCHES_ATTR_REGEXP, args);
    return args.map(function (arg) {
      var _arg$split$;
      return stripQuotes(((_arg$split$ = arg.split(EQUAL_SIGN)[0]) !== null && _arg$split$ !== void 0 ? _arg$split$ : '').trim());
    });
  };

  /**
   * Adds a `:remove-attr()` argument to the observation data.
   *
   * Plain arguments are observed natively by name, while a regexp argument
   * can match any attribute, so the observer watches all attributes
   * and checks them with a matcher.
   *
   * Both the original and the lower-cased spelling of a plain argument
   * are observed: HTML attribute names are lower-cased in mutation records,
   * while SVG ones preserve their case.
   *
   * @param data Mutation observation data.
   * @param arg `:remove-attr()` argument.
   */
  var addActionArg = function addActionArg(data, arg) {
    if (!arg || data.mergedArgs.has(arg)) {
      return;
    }
    data.mergedArgs.add(arg);
    if (REGEXP_ACTION_ARG.test(arg)) {
      // any attribute can match a regexp argument
      data.observeAllAttributes = true;
      data.attributeMatchers.push(createActionArgMatcher(arg));
      return;
    }
    data.attributeNames.add(arg);
    data.selectorAttributeNames.add(arg);
    var lowerCasedArg = arg.toLowerCase();
    if (lowerCasedArg !== arg) {
      data.attributeNames.add(lowerCasedArg);
      data.selectorAttributeNames.add(lowerCasedArg);
    }
  };

  /**
   * Adds a `:matches-attr()` name argument to the observation data.
   *
   * The argument is converted with the same helper as in the selector engine,
   * so the mutation relevance check matches the runtime behavior:
   * plain names are observed natively by name, while regexp and wildcard
   * patterns make the observer watch all attributes.
   *
   * @param data Mutation observation data.
   * @param arg `:matches-attr()` name argument.
   */
  var addSelectorNameArg = function addSelectorNameArg(data, arg) {
    if (!arg || data.mergedArgs.has(arg)) {
      return;
    }
    data.mergedArgs.add(arg);
    var nameMatch;
    try {
      nameMatch = getValidMatcherArg(arg);
    } catch (e) {
      // invalid arguments are rejected by the selector engine at query time
      return;
    }
    if (typeof nameMatch === 'string') {
      // both spellings are observed: HTML attribute names are lower-cased
      // in mutation records, while SVG ones preserve their case
      data.attributeNames.add(nameMatch);
      data.selectorAttributeNames.add(nameMatch);
      var lowerCasedName = nameMatch.toLowerCase();
      if (lowerCasedName !== nameMatch) {
        data.attributeNames.add(lowerCasedName);
        data.selectorAttributeNames.add(lowerCasedName);
      }
      return;
    }

    // a regexp or wildcard name can match any attribute
    data.observeAllAttributes = true;
    data.attributeMatchers.push(function (name) {
      return nameMatch.test(name);
    });
  };

  /**
   * Collects data needed to observe mutations which can affect applied rules.
   *
   * `id` and `class` attributes are always observed as they are used by selectors.
   * Attributes used by action rule selectors and plain `:remove-attr()` arguments
   * are observed natively; a regexp `:remove-attr()` argument or a regexp or
   * wildcard `:matches-attr()` name makes the observer watch all attributes.
   *
   * @param context ExtendedCss context.
   *
   * @returns Data for mutation observation.
   */
  var createMutationObservationData = function createMutationObservationData(context) {
    var data = {
      attributeNames: new Set([ID_ATTRIBUTE, CLASS_ATTRIBUTE]),
      attributeMatchers: [],
      mergedArgs: new Set(),
      selectorAttributeNames: new Set(),
      observeAllAttributes: false
    };
    context.parsedRules.forEach(function (rule) {
      var _rule$actionStyles;
      // only action rules need to react to changes of attributes
      // used by their selectors: actions are re-applied on mutations,
      // while regular rules react only to `id`/`class` changes as before
      if (!((_rule$actionStyles = rule.actionStyles) !== null && _rule$actionStyles !== void 0 && _rule$actionStyles.length)) {
        return;
      }
      getSelectorAttributeNames(rule.selector).forEach(function (name) {
        data.selectorAttributeNames.add(name);
        data.attributeNames.add(name);
      });
      getMatchesAttrNames(rule.selector).forEach(function (name) {
        addSelectorNameArg(data, name);
      });
      rule.actionStyles.forEach(function (actionStyle) {
        if (actionStyle.property !== REMOVE_ATTR_PSEUDO_MARKER) {
          return;
        }
        addActionArg(data, actionStyle.value);
      });
    });
    return data;
  };

  /**
   * Cache of mutation observation data per context:
   * parsed rules are set only in the constructor, so the data is built once.
   */
  var mutationObservationDataCache = new WeakMap();

  /**
   * Returns data needed to observe mutations which can affect applied rules,
   * building it once per context.
   *
   * @param context ExtendedCss context.
   *
   * @returns Data for mutation observation.
   */
  var getMutationObservationData = function getMutationObservationData(context) {
    var data = mutationObservationDataCache.get(context);
    if (!data) {
      data = createMutationObservationData(context);
      mutationObservationDataCache.set(context, data);
    }
    return data;
  };

  /**
   * Adds attributes used by action styles of the given rules
   * to the mutation observation data.
   *
   * Needed because the data is built from parsed rules, while the
   * `beforeStyleApplied` callback can edit or add action arguments:
   * without the merge, mutations of attributes from edited arguments
   * would not be observed and actions would not be re-applied.
   *
   * @param context ExtendedCss context.
   * @param rules Rules data to merge.
   */
  var addRulesToMutationObservation = function addRulesToMutationObservation(context, rules) {
    var data = getMutationObservationData(context);
    rules.forEach(function (rule) {
      var _rule$actionStyles2;
      (_rule$actionStyles2 = rule.actionStyles) === null || _rule$actionStyles2 === void 0 || _rule$actionStyles2.forEach(function (actionStyle) {
        if (actionStyle.property !== REMOVE_ATTR_PSEUDO_MARKER) {
          return;
        }
        addActionArg(data, actionStyle.value);
      });
    });
  };

  /**
   * Creates a checker which decides whether a DOM mutation can affect the applied rules.
   *
   * `id` and `class` attributes are always checked as they are used by selectors,
   * attributes used by action rule selectors are checked,
   * and `remove-attr()` arguments and regexp or wildcard `:matches-attr()` names
   * are checked with the corresponding matchers.
   *
   * @param data Mutation observation data.
   *
   * @returns Mutation relevance checker.
   */
  var createMutationRelevanceChecker = function createMutationRelevanceChecker(data) {
    return function (mutation) {
      if (mutation.type !== 'attributes') {
        return true;
      }
      var attributeName = mutation.attributeName;
      if (!attributeName) {
        return true;
      }
      if (attributeName === ID_ATTRIBUTE || attributeName === CLASS_ATTRIBUTE) {
        return true;
      }
      if (data.selectorAttributeNames.has(attributeName)) {
        return true;
      }
      return data.attributeMatchers.some(function (isMatch) {
        return isMatch(attributeName);
      });
    };
  };

  /**
   * We are trying to limit the number of callback calls by not calling it on all kind of "hover" events.
   * The rationale behind this is that "hover" events often cause attributes modification,
   * but re-applying extCSS rules will be useless as these attribute changes are usually transient.
   *
   * @param mutations DOM elements mutation records.
   * @returns True if all mutations are about attributes changes, otherwise false.
   */
  function shouldIgnoreMutations(mutations) {
    // ignore if all mutations are about attributes changes
    return !mutations.some(function (m) {
      return m.type !== 'attributes';
    });
  }

  /**
   * Adds new `context.domMutationObserver` instance and connect it to document.
   * 
   * @param context ExtendedCss context.
   */
  function observeDocument(context) {
    if (context.isDomObserved) {
      return;
    }

    // enable dynamically added elements handling
    context.isDomObserved = true;
    var observationData = getMutationObservationData(context);
    var isRelevantMutation = createMutationRelevanceChecker(observationData);
    context.domMutationObserver = new natives.MutationObserver(function (mutations) {
      if (!mutations || mutations.length === 0) {
        return;
      }
      // ignore mutations which cannot affect the applied rules
      if (!mutations.some(isRelevantMutation)) {
        return;
      }
      var eventTracker = new EventTracker();
      if (eventTracker.isIgnoredEventType() && shouldIgnoreMutations(mutations)) {
        return;
      }
      // save instance of EventTracker to context
      // for removing its event listeners on disconnectDocument() while mainDisconnect()
      context.eventTracker = eventTracker;
      context.scheduler.run();
    });
    context.domMutationObserver.observe(document, _objectSpread({
      childList: true,
      subtree: true,
      attributes: true
    }, observationData.observeAllAttributes ? {} : {
      attributeFilter: _toConsumableArray(observationData.attributeNames)
    }));
  }

  /**
   * Disconnect from `context.domMutationObserver`.
   * 
   * @param context ExtendedCss context.
   */
  function disconnectDocument(context) {
    if (!context.isDomObserved) {
      return;
    }

    // disable dynamically added elements handling
    context.isDomObserved = false;
    if (context.domMutationObserver) {
      context.domMutationObserver.disconnect();
    }

    // clean up event listeners
    if (context.eventTracker) {
      context.eventTracker.stopTracking();
    }
  }

  // added by tsurlfilter's CssHitsCounter
  var CONTENT_ATTR_PREFIX_REGEXP = /^("|')adguard.+?/;

  /**
   * Checks whether the rule contains regular styles to apply:
   * action pseudo-classes and the internal 'content' of action rules
   * are needed for internal usage and should not be counted as regular styles.
   *
   * @param rule Rule data.
   *
   * @returns True if the rule contains regular styles.
   */
  var hasRegularStyles = function hasRegularStyles(rule) {
    var style = rule.style;
    if (!style) {
      return false;
    }
    return Object.keys(style).some(function (prop) {
      // `remove` is an action marker and is not a CSS property
      if (isActionPseudoMarker(prop)) {
        return false;
      }
      // 'content' of action rules is kept only for `beforeStyleApplied`
      if (prop === CONTENT_CSS_PROPERTY && rule.hasInternalContent) {
        return false;
      }
      return true;
    });
  };

  /**
   * Removes attributes matching the `:remove-attr()` argument from the node.
   *
   * @param node DOM node.
   * @param arg Normalized `:remove-attr()` argument.
   * @param skipStyleAttribute True if the `style` attribute should not be removed.
   *
   * @returns Number of removed attributes.
   */
  var removeAttributes = function removeAttributes(node, arg) {
    var skipStyleAttribute = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : false;
    if (!(node instanceof Element)) {
      return 0;
    }
    var removedCount = 0;
    var isMatch = createActionArgMatcher(arg);
    node.getAttributeNames().forEach(function (name) {
      if (skipStyleAttribute && name === STYLE_ATTRIBUTE) {
        return;
      }
      if (isMatch(name)) {
        node.removeAttribute(name);
        removedCount += 1;
      }
    });
    return removedCount;
  };

  /**
   * Removes classes matching the `:remove-class()` argument from the node.
   *
   * @param node DOM node.
   * @param arg Normalized `:remove-class()` argument.
   *
   * @returns Number of removed classes.
   */
  var removeClasses = function removeClasses(node, arg) {
    if (!(node instanceof Element)) {
      return 0;
    }
    var removedCount = 0;
    var isMatch = createActionArgMatcher(arg);
    // snapshot classes as the live collection is mutated while removing
    _toConsumableArray(node.classList).forEach(function (name) {
      if (isMatch(name)) {
        node.classList.remove(name);
        removedCount += 1;
      }
    });
    return removedCount;
  };

  /**
   * Removes the specified DOM node.
   *
   * @param context ExtendedCss context.
   * @param affectedElement Affected element.
   * @param node Node to remove: the matched node or the one selected by the callback.
   *
   * @returns True if the node was removed from DOM.
   */
  var removeElement = function removeElement(context, affectedElement, node) {
    affectedElement.removed = true;
    var elementSelector = getElementSelectorPath(node);

    // check if the element has been already removed earlier
    var elementRemovalsCounter = context.removalsStatistic[elementSelector] || 0;

    // if removals attempts happened more than specified we do not try to remove node again
    if (elementRemovalsCounter > MAX_STYLE_PROTECTION_COUNT) {
      logger.error("ExtendedCss: infinite loop protection for selector: '".concat(elementSelector, "'"));
      return false;
    }
    if (node.parentElement) {
      node.parentElement.removeChild(node);
      context.removalsStatistic[elementSelector] = elementRemovalsCounter + 1;
      return true;
    }
    return false;
  };

  /**
   * Sets style to the specified DOM node.
   *
   * @param node DOM element.
   * @param style Style to set.
   * @param skipContent True if the `content` property should not be applied:
   * such `content` is kept only for `beforeStyleApplied` (e.g. CssHitsCounter).
   */
  var setStyleToElement = function setStyleToElement(node, style) {
    var skipContent = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : false;
    if (!(node instanceof HTMLElement)) {
      return;
    }
    Object.keys(style).forEach(function (prop) {
      // action pseudo-classes are handled by applyStyle() and are not CSS properties
      if (isActionStyleMarker(prop)) {
        return;
      }
      // do not apply internal 'content' of action rules
      // (e.g. via the style protection observer)
      if (skipContent && prop === CONTENT_CSS_PROPERTY) {
        return;
      }
      // Apply this style only to existing properties
      // We cannot use hasOwnProperty here (does not work in FF)
      if (typeof node.style.getPropertyValue(prop.toString()) !== 'undefined') {
        var value = style[prop];
        if (!value) {
          return;
        }
        // do not apply 'content' style given by tsurlfilter
        // which is needed only for BeforeStyleAppliedCallback
        if (prop === CONTENT_CSS_PROPERTY && value.match(CONTENT_ATTR_PREFIX_REGEXP)) {
          return;
        }
        // First we should remove !important attribute (or it won't be applied')
        value = removeSuffix(value.trim(), '!important').trim();
        node.style.setProperty(prop, value, 'important');
      }
    });
  };

  /**
   * Checks the required properties of `affectedElement`
   * **before** `beforeStyleApplied()` execution.
   *
   * @param affectedElement Affected element.
   *
   * @returns False if there is no `node` or `rules`
   * or `rules` is not an array.
   */
  var isIAffectedElement = function isIAffectedElement(affectedElement) {
    return 'node' in affectedElement && 'rules' in affectedElement && affectedElement.rules instanceof Array;
  };

  /**
   * Checks the required properties of `affectedElement`
   * **after** `beforeStyleApplied()` execution.
   * These properties are needed for proper internal usage.
   *
   * @param affectedElement Affected element.
   *
   * @returns False if there is no `node` or `rules`
   * or `rules` is not an array.
   */
  var isAffectedElement = function isAffectedElement(affectedElement) {
    return 'node' in affectedElement && 'originalStyle' in affectedElement && 'rules' in affectedElement && affectedElement.rules instanceof Array;
  };

  /**
   * Applies action pseudo-class styles of the rule to the specified DOM node.
   *
   * @param context ExtendedCss context.
   * @param node DOM node.
   * @param rule Rule data.
   * @param skipStyleAttribute True if the `style` attribute should not be removed.
   *
   * @returns True if an attribute or a class was removed from DOM.
   */
  var applyRuleAction = function applyRuleAction(context, node, rule, skipStyleAttribute) {
    var _rule$actionStyles;
    var hasRemovals = false;
    (_rule$actionStyles = rule.actionStyles) === null || _rule$actionStyles === void 0 || _rule$actionStyles.forEach(function (actionStyle) {
      var _removalsStatistic$st;
      var statisticKey = "".concat(actionStyle.property, ":").concat(actionStyle.value);
      // the statistic is stored in the context keyed by the node,
      // so it survives affected element re-creation
      // for self-unmatching rules like '.ad:remove-class(ad)'
      var removalsStatistic = context.actionRemovalsStatistic.get(node);
      if (!removalsStatistic) {
        removalsStatistic = {};
        context.actionRemovalsStatistic.set(node, removalsStatistic);
      }
      var removalsCount = (_removalsStatistic$st = removalsStatistic[statisticKey]) !== null && _removalsStatistic$st !== void 0 ? _removalsStatistic$st : 0;
      // limit removals to avoid an endless loop
      // if the page keeps re-adding the attribute or class
      if (removalsCount >= MAX_ACTION_REMOVALS_COUNT) {
        return;
      }
      var removedCount = 0;
      if (actionStyle.property === REMOVE_ATTR_PSEUDO_MARKER) {
        removedCount = removeAttributes(node, actionStyle.value, skipStyleAttribute);
      } else if (actionStyle.property === REMOVE_CLASS_PSEUDO_MARKER) {
        removedCount = removeClasses(node, actionStyle.value);
      }
      if (removedCount > 0) {
        hasRemovals = true;
        removalsStatistic[statisticKey] = removalsCount + 1;
        if (removalsCount + 1 === MAX_ACTION_REMOVALS_COUNT) {
          logger.error("ExtendedCss: infinite loop protection for action '".concat(statisticKey, "' of rule '").concat(rule.selector, "'"));
        }
      }
    });
    return hasRemovals;
  };

  /**
   * Applies regular styles of the affected element's rules.
   *
   * Called for each matched rule while rules are being collected,
   * so selectors which depend on styles applied by other rules
   * can match during the same pass.
   *
   * @param context ExtendedCss context.
   * @param affectedElement Affected element.
   * @throws An error if affectedElement has no style to apply.
   */
  var applyRuleStyles = function applyRuleStyles(context, affectedElement) {
    var _affectedElement$appl;
    var appliedRules = (_affectedElement$appl = affectedElement.appliedRules) !== null && _affectedElement$appl !== void 0 ? _affectedElement$appl : new WeakSet();
    affectedElement.appliedRules = appliedRules;
    var hasUnappliedStyles = affectedElement.rules.some(function (rule) {
      return hasRegularStyles(rule) && !appliedRules.has(rule);
    });
    // actions are re-applied on DOM mutations,
    // so the callback is called again to let it edit or cancel them
    var hasActions = !!affectedElement.hasActionStyles || affectedElement.rules.some(function (rule) {
      var _rule$actionStyles2;
      return !!((_rule$actionStyles2 = rule.actionStyles) !== null && _rule$actionStyles2 !== void 0 && _rule$actionStyles2.length);
    });
    // `:remove()` rules should also reach the callback,
    // so it can filter or cancel the removal
    var hasRemovals = affectedElement.rules.some(function (rule) {
      var _rule$style;
      return ((_rule$style = rule.style) === null || _rule$style === void 0 ? void 0 : _rule$style[REMOVE_PSEUDO_MARKER]) === PSEUDO_PROPERTY_POSITIVE_VALUE;
    });
    // nothing to do for elements with applied regular styles,
    // without actions, removals and new regular styles
    if (affectedElement.isStyleApplied && !hasActions && !hasRemovals && !hasUnappliedStyles) {
      return;
    }
    var beforeStyleApplied = context.beforeStyleApplied;
    var element = affectedElement;
    if (beforeStyleApplied) {
      if (!isIAffectedElement(affectedElement)) {
        throw new Error("Returned IAffectedElement should have 'node' and 'rules' properties");
      }
      var callbackResult = beforeStyleApplied(affectedElement);
      if (!callbackResult) {
        throw new Error("Callback 'beforeStyleApplied' should return IAffectedElement");
      }
      element = callbackResult;
    }
    if (!isAffectedElement(element)) {
      throw new Error("Returned IAffectedElement should have 'node' and 'rules' properties");
    }
    if (beforeStyleApplied) {
      // keep the mutation observer in sync with action arguments
      // which can be edited by the callback
      addRulesToMutationObservation(context, element.rules);
    }
    if (element !== affectedElement) {
      // use rules returned by the callback for the current pass:
      // they are needed for applying actions and for style protection
      affectedElement.rules = element.rules;
    }
    // keep the node selected by the callback:
    // actions and removals are applied to it instead of the matched node
    affectedElement.callbackNode = element.node;
    var _element = element,
      node = _element.node,
      rules = _element.rules;
    var _iterator = _createForOfIteratorHelper(rules),
      _step;
    try {
      for (_iterator.s(); !(_step = _iterator.n()).done;) {
        var rule = _step.value;
        var selector = rule.selector,
          style = rule.style,
          debug = rule.debug;
        // rule may not have style to apply
        // e.g. 'div:has(> a) { debug: true }' -> means no style to apply, and enable debug mode
        if (!style) {
          if (!debug) {
            // but rule should not have both style and debug properties
            throw new Error("No style declaration in rule for selector: '".concat(selector, "'"));
          }
          continue;
        }
        // `:remove()` and action pseudo-classes are applied
        // after all rules are queried (see `applyStyle`)
        if (style[REMOVE_PSEUDO_MARKER] === PSEUDO_PROPERTY_POSITIVE_VALUE) {
          continue;
        }
        if (hasRegularStyles(rule) && !appliedRules.has(rule)) {
          // action markers and internal 'content' are skipped by setStyleToElement,
          // so regular styles of other rules with the same selector are applied
          setStyleToElement(node, style, !!rule.hasInternalContent);
          appliedRules.add(rule);
        }
      }

      // flags are set from the rules which are actually applied,
      // including rules returned by the callback:
      // regular styles should be protected and reverted,
      // and actions require the callback to be called before re-application
    } catch (err) {
      _iterator.e(err);
    } finally {
      _iterator.f();
    }
    var hasAppliedRegularStyles = rules.some(function (rule) {
      return hasRegularStyles(rule);
    });
    affectedElement.hasRegularStyles = !!affectedElement.hasRegularStyles || hasAppliedRegularStyles;
    affectedElement.hasActionStyles = !!affectedElement.hasActionStyles || rules.some(function (rule) {
      var _rule$actionStyles3;
      return !!((_rule$actionStyles3 = rule.actionStyles) !== null && _rule$actionStyles3 !== void 0 && _rule$actionStyles3.length);
    });
    affectedElement.isStyleApplied = !!affectedElement.isStyleApplied || hasAppliedRegularStyles;
  };

  /**
   * Applies `:remove()` pseudo-class and actions of the affected element's rules.
   *
   * Regular styles are applied earlier, while rules are being collected
   * (see `applyRuleStyles`), so selectors which depend on applied styles
   * can match during the same pass.
   *
   * @param context ExtendedCss context.
   * @param rawAffectedElement Object containing DOM node and rules to be applied.
   *
   * @returns True if the DOM was changed by a removal.
   */
  var applyStyle = function applyStyle(context, rawAffectedElement) {
    var _rawAffectedElement$c;
    var rules = rawAffectedElement.rules;
    // actions and removals are applied to the node selected by the callback
    // (it is the matched node if the callback did not replace it)
    var node = (_rawAffectedElement$c = rawAffectedElement.callbackNode) !== null && _rawAffectedElement$c !== void 0 ? _rawAffectedElement$c : rawAffectedElement.node;
    // only actions and `:remove()` rules require processing
    // after regular styles are applied
    var hasActions = rules.some(function (rule) {
      var _rule$actionStyles4, _rule$style2;
      return !!((_rule$actionStyles4 = rule.actionStyles) !== null && _rule$actionStyles4 !== void 0 && _rule$actionStyles4.length) || ((_rule$style2 = rule.style) === null || _rule$style2 === void 0 ? void 0 : _rule$style2[REMOVE_PSEUDO_MARKER]) === PSEUDO_PROPERTY_POSITIVE_VALUE;
    });
    if (!hasActions) {
      return false;
    }

    // the `style` attribute should not be removed from elements
    // with protected regular styles, including the first pass;
    // the flag is set by `applyRuleStyles()` for every affected element
    // before actions are applied, and there is at most one affected element per node
    var skipStyleAttribute = !!rawAffectedElement.hasRegularStyles;
    var hasRemovals = false;
    var _iterator2 = _createForOfIteratorHelper(rules),
      _step2;
    try {
      for (_iterator2.s(); !(_step2 = _iterator2.n()).done;) {
        var rule = _step2.value;
        var style = rule.style;
        if (!style) {
          continue;
        }
        if (style[REMOVE_PSEUDO_MARKER] === PSEUDO_PROPERTY_POSITIVE_VALUE) {
          return removeElement(context, rawAffectedElement, node);
        }
        hasRemovals = applyRuleAction(context, node, rule, skipStyleAttribute) || hasRemovals;
      }
    } catch (err) {
      _iterator2.e(err);
    } finally {
      _iterator2.f();
    }
    return hasRemovals;
  };

  /**
   * Reverts style for the affected object.
   *
   * @param affectedElement Affected element.
   */
  var revertStyle = function revertStyle(affectedElement) {
    if (affectedElement.protectionObserver) {
      affectedElement.protectionObserver.disconnect();
    }
    affectedElement.node.style.cssText = affectedElement.originalStyle;
  };

  /**
   * ExtMutationObserver is a wrapper over regular MutationObserver with one additional function:
   * it keeps track of the number of times we called the "ProtectionCallback".
   *
   * We use an instance of this to monitor styles added by ExtendedCss
   * and to make sure these styles are recovered if the page script attempts to modify them.
   *
   * However, we want to avoid endless loops of modification if the page script repeatedly modifies the styles.
   * So we keep track of the number of calls and observe() makes a decision
   * whether to continue recovering the styles or not.
   */
  var ExtMutationObserver = /*#__PURE__*/function () {
    /**
     * Extra property for keeping 'style fix counts'.
     */

    /**
     * Creates new ExtMutationObserver.
     *
     * @param protectionCallback Callback which execution should be counted.
     */
    function ExtMutationObserver(protectionCallback) {
      var _this6 = this;
      _classCallCheck(this, ExtMutationObserver);
      this.styleProtectionCount = 0;
      this.observer = new natives.MutationObserver(function (mutations) {
        if (!mutations.length) {
          return;
        }
        _this6.styleProtectionCount += 1;
        protectionCallback(mutations, _this6);
      });
    }

    /**
     * Starts to observe target element,
     * prevents infinite loop of observing due to the limited number of times of callback runs.
     *
     * @param target Target to observe.
     * @param options Mutation observer options.
     */
    return _createClass(ExtMutationObserver, [{
      key: "observe",
      value: function observe(target, options) {
        if (this.styleProtectionCount < MAX_STYLE_PROTECTION_COUNT) {
          this.observer.observe(target, options);
        } else {
          logger.error('ExtendedCss: infinite loop protection for style');
        }
      }

      /**
       * Stops ExtMutationObserver from observing any mutations.
       * Until the `observe()` is used again, `protectionCallback` will not be invoked.
       */
    }, {
      key: "disconnect",
      value: function disconnect() {
        this.observer.disconnect();
      }
    }]);
  }();
  var PROTECTION_OBSERVER_OPTIONS = {
    attributes: true,
    attributeOldValue: true,
    attributeFilter: [STYLE_ATTRIBUTE]
  };

  /**
   * Style map with a flag indicating whether its `content` should be applied.
   */

  /**
   * Creates MutationObserver protection callback.
   *
   * @param styles Styles data objects.
   *
   * @returns Callback for styles protection.
   */
  var createProtectionCallback = function createProtectionCallback(styles) {
    var protectionCallback = function protectionCallback(mutations, extObserver) {
      if (!mutations[0]) {
        return;
      }
      var target = mutations[0].target;
      extObserver.disconnect();
      styles.forEach(function (_ref) {
        var style = _ref.style,
          skipContent = _ref.skipContent;
        setStyleToElement(target, style, skipContent);
      });
      extObserver.observe(target, PROTECTION_OBSERVER_OPTIONS);
    };
    return protectionCallback;
  };

  /**
   * Sets up a MutationObserver which protects style attributes from changes.
   *
   * @param node DOM node.
   * @param rules Rule data objects.
   * @returns Mutation observer used to protect attribute or null if there's nothing to protect.
   */
  var protectStyleAttribute = function protectStyleAttribute(node, rules) {
    if (!natives.MutationObserver) {
      return null;
    }
    var styles = [];
    rules.forEach(function (ruleData) {
      var style = ruleData.style;
      // some rules might have only debug property in style declaration
      // e.g. 'div:has(> a) { debug: true }' -> parsed to boolean `ruleData.debug`
      // so no style is fine, and here we should collect only valid styles to protect
      if (style) {
        styles.push({
          style: style,
          skipContent: !!ruleData.hasInternalContent
        });
      }
    });
    var protectionObserver = new ExtMutationObserver(createProtectionCallback(styles));
    protectionObserver.observe(node, PROTECTION_OBSERVER_OPTIONS);
    return protectionObserver;
  };
  var STATS_DECIMAL_DIGITS_COUNT = 4;
  /**
   * A helper class for applied rule stats.
   */
  var TimingStats = /*#__PURE__*/function () {
    /**
     * Creates new TimingStats.
     */
    function TimingStats() {
      _classCallCheck(this, TimingStats);
      this.appliesTimings = [];
      this.appliesCount = 0;
      this.timingsSum = 0;
      this.meanTiming = 0;
      this.squaredSum = 0;
      this.standardDeviation = 0;
    }

    /**
     * Observe target element and mark observer as active.
     *
     * @param elapsedTimeMs Time in ms.
     */
    return _createClass(TimingStats, [{
      key: "push",
      value: function push(elapsedTimeMs) {
        this.appliesTimings.push(elapsedTimeMs);
        this.appliesCount += 1;
        this.timingsSum += elapsedTimeMs;
        this.meanTiming = this.timingsSum / this.appliesCount;
        this.squaredSum += elapsedTimeMs * elapsedTimeMs;
        this.standardDeviation = Math.sqrt(this.squaredSum / this.appliesCount - Math.pow(this.meanTiming, 2));
      }
    }]);
  }();
  /**
   * Makes the timestamps more readable.
   *
   * @param timestamp Raw timestamp.
   *
   * @returns Fine-looking timestamps.
   */
  var beautifyTimingNumber = function beautifyTimingNumber(timestamp) {
    return Number(timestamp.toFixed(STATS_DECIMAL_DIGITS_COUNT));
  };

  /**
   * Improves timing stats readability.
   *
   * @param rawTimings Collected timings with raw timestamp.
   *
   * @returns Fine-looking timing stats.
   */
  var beautifyTimings = function beautifyTimings(rawTimings) {
    return {
      appliesTimings: rawTimings.appliesTimings.map(function (t) {
        return beautifyTimingNumber(t);
      }),
      appliesCount: beautifyTimingNumber(rawTimings.appliesCount),
      timingsSum: beautifyTimingNumber(rawTimings.timingsSum),
      meanTiming: beautifyTimingNumber(rawTimings.meanTiming),
      standardDeviation: beautifyTimingNumber(rawTimings.standardDeviation)
    };
  };

  /**
   * Prints timing information if debugging mode is enabled.
   *
   * @param context ExtendedCss context.
   */
  var printTimingInfo = function printTimingInfo(context) {
    if (context.areTimingsPrinted) {
      return;
    }
    context.areTimingsPrinted = true;
    var timingsLogData = {};
    context.parsedRules.forEach(function (ruleData) {
      if (ruleData.timingStats) {
        var selector = ruleData.selector,
          style = ruleData.style,
          debug = ruleData.debug,
          matchedElements = ruleData.matchedElements;
        // style declaration for some rules is parsed to debug property and no style to apply
        // e.g. 'div:has(> a) { debug: true }'
        if (!style && !debug) {
          throw new Error("Rule should have style declaration for selector: '".concat(selector, "'"));
        }
        var selectorData = {
          selectorParsed: selector,
          timings: beautifyTimings(ruleData.timingStats)
        };
        // `ruleData.style` may contain `remove` pseudo-property
        // and make logs look better
        if (style && style[REMOVE_PSEUDO_MARKER] === PSEUDO_PROPERTY_POSITIVE_VALUE) {
          selectorData.removed = true;
          // no matchedElements for such case as they are removed after ExtendedCss applied
        } else {
          selectorData.styleApplied = style || null;
          selectorData.matchedElements = matchedElements;
        }
        timingsLogData[selector] = selectorData;
      }
    });
    if (Object.keys(timingsLogData).length === 0) {
      return;
    }
    // add location.href to the message to distinguish frames
    logger.info('[ExtendedCss] Timings in milliseconds for %o:\n%o', window.location.href, timingsLogData);
  };

  /**
   * Finds affectedElement object for the specified DOM node.
   *
   * @param affElements Array of affected elements — context.affectedElements.
   * @param domNode DOM node.
   * @returns Found affectedElement or undefined.
   */
  var findAffectedElement = function findAffectedElement(affElements, domNode) {
    return affElements.find(function (affEl) {
      return affEl.node === domNode;
    });
  };

  /**
   * Applies specified rule and returns list of elements affected.
   *
   * @param context ExtendedCss context.
   * @param ruleData Rule to apply.
   * @returns List of elements affected by the rule.
   */
  var applyRule = function applyRule(context, ruleData) {
    // debugging mode can be enabled in two ways:
    // 1. for separate rules - by `{ debug: true; }`
    // 2. for all rules simultaneously by:
    //   - `{ debug: global; }` in any rule
    //   - positive `debug` property in ExtCssConfiguration
    var isDebuggingMode = !!ruleData.debug || context.debug;
    var startTime;
    if (isDebuggingMode) {
      startTime = performance.now();
    }
    var ast = ruleData.ast;
    var nodes = [];
    // selector can be successfully parser into ast with no error
    // but its applying by native Document.querySelectorAll() still may throw an error
    // e.g. 'div[..banner]'
    try {
      nodes.push.apply(nodes, _toConsumableArray(selectElementsByAst(ast)));
    } catch (e) {
      // log the error only in debug mode
      if (context.debug) {
        logger.error(getErrorMessage(e));
      }
    }
    nodes.forEach(function (node) {
      var affectedElement = findAffectedElement(context.affectedElements, node);
      if (affectedElement) {
        affectedElement.rules.push(ruleData);
      } else {
        // element is affected first time
        affectedElement = {
          node: node,
          // affected DOM node
          rules: [ruleData],
          // rule to be applied
          originalStyle: node.style.cssText,
          // original node style
          protectionObserver: null,
          // style attribute observer
          appliedRules: new WeakSet()
        };
        context.affectedElements.push(affectedElement);
      }

      // apply regular styles of the rule right away,
      // so selectors which depend on applied styles can match in the same pass;
      // actions are applied after all rules are queried
      applyRuleStyles(context, affectedElement);
    });
    if (isDebuggingMode && startTime) {
      var elapsedTimeMs = performance.now() - startTime;
      if (!ruleData.timingStats) {
        ruleData.timingStats = new TimingStats();
      }
      ruleData.timingStats.push(elapsedTimeMs);
    }
    return nodes;
  };

  /**
   * Applies filtering rules once.
   *
   * @param context ExtendedCss context.
   *
   * @returns True if the pass changed the DOM with removals.
   */
  var applyRulesPass = function applyRulesPass(context) {
    var hasDomMutations = false;
    var newSelectedElements = new Set();
    // reset rules collected during the previous pass:
    // only rules matching elements in the current pass should be applied,
    // otherwise actions could be replayed for elements which no longer match;
    // the callback-selected node is also reset and set again
    // if the callback is called during this pass
    context.affectedElements.forEach(function (affectedElement) {
      affectedElement.rules = [];
      affectedElement.callbackNode = undefined;
    });
    // some rules could make call - selector.querySelectorAll() temporarily to change node id attribute
    // this caused MutationObserver to call recursively
    // https://github.com/AdguardTeam/ExtendedCss/issues/81
    disconnectDocument(context);
    context.parsedRules.forEach(function (ruleData) {
      var nodes = applyRule(context, ruleData);
      nodes.forEach(function (node) {
        return newSelectedElements.add(node);
      });
      // save matched elements to ruleData as linked to applied rule
      // only for debugging purposes
      if (ruleData.debug) {
        ruleData.matchedElements = nodes;
      }
    });
    // apply collected actions to each affected element once per pass:
    // regular styles and the sticky regular-styles flag
    // are handled by `applyRuleStyles()` while rules are collected
    context.affectedElements.forEach(function (affectedElement) {
      if (!newSelectedElements.has(affectedElement.node)) {
        return;
      }
      hasDomMutations = applyStyle(context, affectedElement) || hasDomMutations;
      // add style protection observer only for elements with regular styles
      if (!affectedElement.removed && affectedElement.hasRegularStyles && !affectedElement.protectionObserver) {
        affectedElement.protectionObserver = protectStyleAttribute(affectedElement.node, affectedElement.rules);
      }
    });

    // Now revert styles for elements which are no more affected
    var affLength = context.affectedElements.length;
    // do nothing if there is no elements to process
    while (affLength) {
      var affectedElement = context.affectedElements[affLength - 1];
      if (!affectedElement) {
        break;
      }
      if (!newSelectedElements.has(affectedElement.node)) {
        // revert styles only if the element had regular styles applied
        if (affectedElement.hasRegularStyles) {
          revertStyle(affectedElement);
        }
        context.affectedElements.splice(affLength - 1, 1);
      }
      affLength -= 1;
    }
    return hasDomMutations;
  };

  /**
   * Applies filtering rules.
   *
   * A pass can change the DOM: removed elements or attributes can make
   * other elements match, e.g. `#child:remove()` makes its parent match `:empty`.
   * The document observer is disconnected during the pass,
   * so such changes should be handled by re-running the pass.
   *
   * @param context ExtendedCss context.
   */
  var applyRules = function applyRules(context) {
    while (applyRulesPass(context)) {
      // repeat while the pass changes the DOM
    }
    // After styles are applied we can start observe again
    observeDocument(context);
    printTimingInfo(context);
  };

  /**
   * Result of selector validation.
   */

  /**
   * Interface for ExtendedCss constructor argument. Needed to create the instance of ExtendedCss.
   *
   * ExtendedCss configuration may contain:
   * - `styleSheet` — CSS stylesheet of rules to apply;
   * - `cssRules` — list of separated CSS rules instead of or additionally to the CSS stylesheet;
   * - `beforeStyleApplied` — the callback for matched elements;
   * - `debug` — flag for global logging which is useful for selectors debugging.
   *
   * > Both `styleSheet` and `cssRules` are optional but at least one of them should be set.
   *
   * > If both `styleSheet` and `cssRules` are set, both of them are to be applied.
   *
   * CSS stylesheet and list of CSS rules can contain not just extended selectors
   * but standard ones, and all of them will be applied by the lib.
   *
   * CSS comments and at-rules are not supported.
   */

  /**
   * Main class of ExtendedCss lib.
   *
   * Parses css stylesheet with any selectors (passed to its argument as styleSheet),
   * and guarantee its applying as mutation observer is used to prevent the restyling of needed elements by other scripts.
   * This style protection is limited to 50 times to avoid infinite loop (MAX_STYLE_PROTECTION_COUNT).
   * Our own ThrottleWrapper is used for styles applying to avoid too often lib reactions on page mutations.
   *
   * Constructor creates the instance of class which should be run be `apply()` method to apply the rules,
   * and the applying can be stopped by `dispose()`.
   *
   * Can be used to select page elements by selector with `query()` method (similar to `Document.querySelectorAll()`),
   * which does not require instance creating.
   */
  var ExtendedCss = /*#__PURE__*/function () {
    /**
     * Creates new ExtendedCss.
     *
     * @param configuration ExtendedCss configuration.
     */
    function ExtendedCss(configuration) {
      _classCallCheck(this, ExtendedCss);
      if (!configuration) {
        throw new Error('ExtendedCss configuration should be provided.');
      }
      this.applyRulesCallbackListener = this.applyRulesCallbackListener.bind(this);
      this.context = {
        beforeStyleApplied: configuration.beforeStyleApplied,
        debug: false,
        affectedElements: [],
        isDomObserved: false,
        removalsStatistic: {},
        actionRemovalsStatistic: new WeakMap(),
        parsedRules: [],
        scheduler: new ThrottleWrapper(this.applyRulesCallbackListener)
      };

      // TODO: throw an error instead of logging and handle it in related products.
      if (!isBrowserSupported()) {
        logger.error('Browser is not supported by ExtendedCss');
        return;
      }

      // at least 'styleSheet' or 'cssRules' should be provided
      if (!configuration.styleSheet && !configuration.cssRules) {
        throw new Error("ExtendedCss configuration should have 'styleSheet' or 'cssRules' defined.");
      }

      // 'styleSheet' and 'cssRules' are optional
      // and both can be provided at the same time
      // so both should be parsed and applied in such case
      if (configuration.styleSheet) {
        // stylesheet parsing can fail on some invalid selectors
        try {
          var _this$context$parsedR;
          (_this$context$parsedR = this.context.parsedRules).push.apply(_this$context$parsedR, _toConsumableArray(parseStylesheet(configuration.styleSheet, extCssDocument)));
        } catch (e) {
          // eslint-disable-next-line max-len
          throw new Error("Pass the rules as configuration.cssRules since configuration.styleSheet cannot be parsed because of: '".concat(getErrorMessage(e), "'"));
        }
      }
      if (configuration.cssRules) {
        var _this$context$parsedR2;
        (_this$context$parsedR2 = this.context.parsedRules).push.apply(_this$context$parsedR2, _toConsumableArray(parseRules(configuration.cssRules, extCssDocument)));
      }

      // true if set in configuration
      // or any rule in styleSheet has `debug: global`
      this.context.debug = configuration.debug || this.context.parsedRules.some(function (ruleData) {
        return ruleData.debug === DEBUG_PSEUDO_PROPERTY_GLOBAL_VALUE;
      });
      if (this.context.beforeStyleApplied && typeof this.context.beforeStyleApplied !== 'function') {
        // eslint-disable-next-line max-len
        throw new Error("Invalid configuration. Type of 'beforeStyleApplied' should be a function, received: '".concat(_typeof2(this.context.beforeStyleApplied), "'"));
      }
    }

    /**
     * Invokes {@link applyRules} function with current app context.
     * 
     * This method is bound to the class instance in the constructor because it is called
     * in {@link ThrottleWrapper} and on the DOMContentLoaded event.
     */
    return _createClass(ExtendedCss, [{
      key: "applyRulesCallbackListener",
      value: function applyRulesCallbackListener() {
        applyRules(this.context);
      }

      /**
       * Initializes ExtendedCss.
       *
       * Should be executed on page ASAP,
       * otherwise the :contains() pseudo-class may work incorrectly.
       */
    }, {
      key: "init",
      value: function init() {
        /**
         * Native Node textContent getter must be intercepted as soon as possible,
         * and stored as it is needed for proper work of :contains() pseudo-class
         * because DOM Node prototype 'textContent' property may be mocked.
         *
         * @see {@link https://github.com/AdguardTeam/ExtendedCss/issues/127}
         */
        nativeTextContent.setGetter();
      }

      /**
       * Applies stylesheet rules on page.
       */
    }, {
      key: "apply",
      value: function apply() {
        applyRules(this.context);
        if (document.readyState !== 'complete') {
          document.addEventListener('DOMContentLoaded', this.applyRulesCallbackListener, false);
        }
      }

      /**
       * Disposes ExtendedCss and removes our styles from matched elements.
       */
    }, {
      key: "dispose",
      value: function dispose() {
        disconnectDocument(this.context);
        this.context.affectedElements.forEach(function (el) {
          // action-only elements were not modified by us, so they should not be reverted
          if (el.hasRegularStyles) {
            revertStyle(el);
          }
        });
        // reset action removals statistic, so actions can be applied
        // by `apply()` again after the instance was disposed
        this.context.actionRemovalsStatistic = new WeakMap();
        document.removeEventListener('DOMContentLoaded', this.applyRulesCallbackListener, false);
      }

      /**
       * Exposed for testing purposes only.
       *
       * @returns Array of AffectedElement data objects.
       */
    }, {
      key: "getAffectedElements",
      value: function getAffectedElements() {
        return this.context.affectedElements;
      }

      /**
       * Returns a list of the document's elements that match the specified selector.
       * Uses ExtCssDocument.querySelectorAll().
       *
       * @param selector Selector text.
       * @param noTiming If true, do not print the timings to the console (defaults to true).
       *
       * @throws An error if selector is not valid.
       * @returns A list of elements that match the selector.
       */
    }], [{
      key: "query",
      value: function query(selector) {
        var noTiming = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : true;
        if (typeof selector !== 'string') {
          throw new Error('Selector should be defined as a string.');
        }
        var start = performance.now();
        try {
          return extCssDocument.querySelectorAll(selector);
        } finally {
          var end = performance.now();
          if (!noTiming) {
            logger.info("[ExtendedCss] Elapsed: ".concat(Math.round((end - start) * 1000), " \u03BCs."));
          }
        }
      }

      /**
       * Validates selector.
       *
       * @param inputSelector Selector text to validate.
       *
       * @returns Result of selector validation.
       */
    }, {
      key: "validate",
      value: function validate(inputSelector) {
        try {
          // ExtendedCss in general supports action pseudo-classes in a selector
          // but ExtendedCss.query() does not support them as they should be parsed by stylesheet parser.
          // so for validation we have to handle selectors with action pseudo-classes in them
          var _parseActionSelector = parseActionSelector(inputSelector),
            selector = _parseActionSelector.selector;
          ExtendedCss.query(selector);
          return {
            ok: true,
            error: null
          };
        } catch (e) {
          // not valid input `selector` should be logged eventually
          var error = "Error: Invalid selector: '".concat(inputSelector, "' -- ").concat(getErrorMessage(e));
          return {
            ok: false,
            error: error
          };
        }
      }
    }]);
  }();
  return ExtendedCss;
}();