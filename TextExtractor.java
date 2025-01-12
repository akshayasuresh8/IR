package org.example;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.*;

public class TextExtractor {

    /*
    * @param File
    * @Output String
    * */
    public String extract(String file) throws IOException {
        StringBuilder string = new StringBuilder();
        try(StandardAnalyzer analyzer = new StandardAnalyzer()){

        TokenStream tokens = analyzer.tokenStream("text",new BufferedReader(new FileReader(file)));
        CharTermAttribute termAttr = tokens.addAttribute(CharTermAttribute.class);
        tokens.reset();
        while (tokens.incrementToken()){
            String s =  termAttr.toString();
            string.append(s).append(" ");

        }


        }
        return string.toString();
    }

    public static void main(String[] args) throws IOException {
        TextExtractor tx = new TextExtractor();
        System.out.println(tx.extract("example\\T7.txt"));

    }
}
