package com.overviewdocs.nlp

import com.overviewdocs.nlp.DocumentVectorTypes.TermWeight
import org.specs2.mutable.Specification

class WeightedLexerSpec extends Specification {
  val stopWords = Set("no", "i", "you", "the", "are")

  def makeWeightedTermSeq(s:Seq[(String,TermWeight)]) = s map { case (t,w) => WeightedTermString(t,w) }

  "WeightedLexer wihout custom weights" should {
    val wl = new WeightedLexer(
      stopWords,
      Map("cat\\b"->5, "cats+"->13, "\\w*CAT\\w*"->10, "\\w*DOG\\w*"->10)
    )

    "remove stop words when no matches" in {
      wl.makeTerms("no i haha you") must beEqualTo(makeWeightedTermSeq(Seq(("haha",1))))
    }

    "truncate long words" in {
      val longword = "thequickbrownfoxjumpsoverthelazydogthequickbrownfoxjumpsoverthelazydogthequickbrownfoxjumpsoverthelazydogthequickbrownfoxjumpsoverthelazydog"
      val sentence = "now is the time for all good " + longword + " to come to the aid of their module."
      wl.makeTerms(sentence).map(_.term.length).max must beEqualTo(wl.maxTokenLength)
    }

    "match one simple pattern" in {
      val sentence = "the cat likes tea"
      val terms = makeWeightedTermSeq(Seq(("cat",5), ("likes",1), ("tea", 1)))
      wl.makeTerms(sentence) must beEqualTo(terms)
    }

    "match a regex" in {
      val sentence = "catsss like tea"
      val terms = makeWeightedTermSeq(Seq(("catsss",13), ("like",1), ("tea", 1)))
      wl.makeTerms(sentence) must beEqualTo(terms)
    }

    "match uppercase in a regex" in {
      val sentence = "you sir are a weirdCATandDOGmongrel"
      val terms = makeWeightedTermSeq(Seq(("sir",1), ("weirdCATandDOGmongrel", 100)))
      wl.makeTerms(sentence) must beEqualTo(terms)
    }
    
    "throw if regex is invalid" in {
      val sentence = "cats are cats all over the world"
        
      val badWl = new WeightedLexer(stopWords, Map("**" -> 100))
      
      badWl.makeTerms(sentence) must throwA[Exception]
    }
  }
}
