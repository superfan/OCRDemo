package com.baidu.ocr.demo;

import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.GeneralResult;
import com.baidu.ocr.sdk.model.VertexesLocation;
import com.baidu.ocr.sdk.model.Word;
import com.baidu.ocr.sdk.utils.Parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class ImageResultParser implements Parser<GeneralResult> {
    public ImageResultParser() {
    }

    public GeneralResult parse(String json) throws OCRError {
        OCRError error;
        try {
            JSONObject e = new JSONObject(json);
            if(e.has("error_code")) {
                error = new OCRError(e.optInt("error_code"), e.optString("error_msg"));
                throw error;
            } else {
                GeneralResult var19 = new GeneralResult();
                var19.setLogId(e.optLong("log_id"));
                var19.setJsonRes(json);
                var19.setDirection(e.optInt("direction", -1));
                var19.setWordsResultNumber(e.optInt("words_result_num"));
                JSONArray wordsArray = e.optJSONArray("words_result");
                int wordsArrayCount = wordsArray == null?0:wordsArray.length();
                ArrayList wordList = new ArrayList();

                for(int i = 0; i < wordsArrayCount; ++i) {
                    JSONObject wordObject = wordsArray.optJSONObject(i);
                    //JSONObject locationObject = wordObject.optJSONObject("location");
                    Word word = new Word();
//                    word.getLocation().setLeft(locationObject.optInt("left"));
//                    word.getLocation().setTop(locationObject.optInt("top"));
//                    word.getLocation().setWidth(locationObject.optInt("width"));
//                    word.getLocation().setHeight(locationObject.optInt("height"));
                    word.setWords(wordObject.optString("words"));
                    wordList.add(word);
//                    JSONArray vertexesLocationArray = wordObject.optJSONArray("vertexes_location");
//                    if(vertexesLocationArray != null) {
//                        ArrayList charArray = new ArrayList();
//
//                        for(int charList = 0; charList < vertexesLocationArray.length(); ++charList) {
//                            JSONObject j = vertexesLocationArray.optJSONObject(charList);
//                            VertexesLocation charObject = new VertexesLocation();
//                            charObject.setX(j.optInt("x"));
//                            charObject.setY(j.optInt("y"));
//                            charArray.add(charObject);
//                        }
//
//                        word.setVertexesLocations(charArray);
//                    }
//
//                    JSONArray var20 = wordObject.optJSONArray("chars");
//                    if(var20 != null) {
//                        ArrayList var21 = new ArrayList();
//
//                        for(int var22 = 0; var22 < var20.length(); ++var22) {
//                            JSONObject var23 = var20.optJSONObject(var22);
//                            JSONObject location = var23.optJSONObject("location");
//                            Word.Char characterResult = new Word.Char();
//                            characterResult.getLocation().setLeft(location.optInt("left"));
//                            characterResult.getLocation().setTop(location.optInt("top"));
//                            characterResult.getLocation().setWidth(location.optInt("width"));
//                            characterResult.getLocation().setHeight(location.optInt("height"));
//                            characterResult.setCharacter(var23.optString("char"));
//                            var21.add(characterResult);
//                        }
//
//                        word.setCharacterResults(var21);
//                    }
                }

                var19.setWordList(wordList);
                return var19;
            }
        } catch (JSONException var18) {
            error = new OCRError(283505, "Server illegal response " + json, var18);
            throw error;
        }
    }
}
