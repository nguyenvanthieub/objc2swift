/**
 * This file is part of objc2swift.
 * https://github.com/yahoojapan/objc2swift
 *
 * Copyright (c) 2015 Yahoo Japan Corporation
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

import ObjCParser._
import org.antlr.v4.runtime._
import org.antlr.v4.runtime.tree.TerminalNode
import collection.JavaConversions._

trait ExpressionVisitor extends Converter {

  self: ObjCBaseVisitor[String] =>

  def visitBinaryExpression(ctx: ParserRuleContext): String = {
    val sb = new StringBuilder()

    for (element <- ctx.children) {
      element match {
        case symbol: TerminalNode => sb.append(" " + symbol.getSymbol.getText + " ")
        case _ => sb.append(visit(element))
      }
    }

    sb.toString()
  }

  def visitUnaryExpression(ctx: ParserRuleContext): String = {
    val sb = new StringBuilder()

    for (element <- ctx.children) {
      element match {
        case symbol: TerminalNode => sb.append(symbol.getSymbol.getText)
        case _ => sb.append(visit(element))
      }
    }

    sb.toString()
  }

  override def visitMessage_expression(ctx: Message_expressionContext): String = {
    val sel = ctx.message_selector()

    val sb = new StringBuilder()
    sb.append(visit(ctx.receiver))
    sb.append(".")

    if(sel.keyword_argument.length == 0) { // no argument
      sb.append(sel.selector.getText + "()")
    } else {
      for (i <- 0 until sel.keyword_argument.length) {
        val arg = sel.keyword_argument(i)
        if(i > 0)
          sb.append(", ")

        if(i == 0) {
          sb.append(arg.selector().getText)
          sb.append("(")
          sb.append(visit(arg.expression))
        } else {
          sb.append(arg.selector().getText + ": ")
          sb.append(visit(arg.expression))
        }
      }
      sb.append(")")
    }
    sb.toString()
  }


  override def visitArray_expression(ctx: Array_expressionContext) = {
    val sb = new StringBuilder()
    sb.append("[")
    sb.append(ctx.postfix_expression.map(visit).mkString(", "))
    sb.append("]")
    sb.toString()
  }


  override def visitDictionary_expression(ctx: Dictionary_expressionContext) = {
    val sb = new StringBuilder()
    sb.append("[")
    sb.append(ctx.dictionary_pair.map(visit).mkString(", "))
    sb.append("]")
    sb.toString()
  }

  override def visitDictionary_pair(ctx: Dictionary_pairContext) = {
    visit(ctx.postfix_expression(0)) + " : " + visit(ctx.postfix_expression(1))
  }


  override def visitBox_expression(ctx: Box_expressionContext): String = {
    Option(ctx.constant) match {
      case Some(const) => return visit(const)
      case None =>
    }

    Option(ctx.postfix_expression) match {
      case Some(expr) => return visit(expr)
      case None =>
    }

    ""
  }

  override def visitBlock_expression(ctx: Block_expressionContext): String = {
    val sb = new StringBuilder()
    sb.append("{")

    if(ctx.block_parameters != null && ctx.type_specifier != null)
      sb.append(visit(ctx.block_parameters) + " -> " + visit(ctx.type_specifier) + " in\n")
    else if(ctx.type_specifier != null)
      sb.append("() -> " + visit(ctx.type_specifier) + " in\n")
    else if(ctx.block_parameters != null )
      sb.append(visit(ctx.block_parameters) + " in\n")
    else
      sb.append("\n")

    sb.append(visit(ctx.compound_statement) + "\n")
    sb.append(indent(ctx) + "}")

    sb.toString()
  }

  override def visitBlock_parameters(ctx: Block_parametersContext): String = {
    "(" + ctx.type_variable_declarator.map(visit).mkString(", ") + ")"
  }


  override def visitConditional_expression(ctx: Conditional_expressionContext): String = {
    if(ctx.getChildCount > 1) {
      visit(ctx.getChild(0)) + " ? " + visit(ctx.getChild(2)) + " : " + visit(ctx.getChild(4))
    } else {
      visit(ctx.getChild(0))
    }
  }

  override def visitPrimary_expression(ctx: Primary_expressionContext): String = {
    if(ctx.getChildCount == 3 && ctx.getChild(0).getText == "(" && ctx.getChild(2).getText == ")") {
      "(" + visit(ctx.getChild(1)) + ")"
    }

    else if (ctx.IDENTIFIER != null) {
      ctx.IDENTIFIER.getText match {
        case "YES" => "true"
        case "NO"  => "false"
        case other => other
      }
    }

    else if(ctx.STRING_LITERAL != null) {
      ctx.STRING_LITERAL.getText.substring(1)
    }

    else if(ctx.constant != null) {
      ctx.constant.getText
    }

    else {
      ctx.getText match {
        case x @ ("self" | "super") => x
        case _ => visitChildren(ctx)
      }
    }
  }

  override def visitExpression(ctx: ExpressionContext) = concatChildResults(ctx, "")
  override def visitArgument_expression_list(ctx: Argument_expression_listContext) = concatChildResults(ctx, ", ")
  override def visitAssignment_expression(ctx: Assignment_expressionContext) = concatChildResults(ctx, " ")

  override def visitEquality_expression(ctx: Equality_expressionContext)       = visitBinaryExpression(ctx)
  override def visitRelational_expression(ctx: Relational_expressionContext)   = visitBinaryExpression(ctx)
  override def visitLogical_or_expression(ctx: Logical_or_expressionContext)   = visitBinaryExpression(ctx)
  override def visitLogical_and_expression(ctx: Logical_and_expressionContext) = visitBinaryExpression(ctx)
  override def visitAdditive_expression(ctx: Additive_expressionContext)       = visitBinaryExpression(ctx)
  override def visitMultiplicative_expression(ctx: Multiplicative_expressionContext) = visitBinaryExpression(ctx)

  override def visitUnary_expression(ctx: Unary_expressionContext)             = visitUnaryExpression(ctx)
  override def visitPostfix_expression(ctx: Postfix_expressionContext)         = visitUnaryExpression(ctx)
}