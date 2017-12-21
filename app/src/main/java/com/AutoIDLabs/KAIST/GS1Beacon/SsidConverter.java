package com.AutoIDLabs.KAIST.GS1Beacon;

import java.math.BigInteger;

/**
 * Created by neochae on 2017. 12. 22..
 */

public class SsidConverter {
    public int mCodeBase = (int)'!';
    public int mCodeNum = 90;

    public SsidConverter(char codeBase, int codeNum) {
        mCodeBase = (int)codeBase;
        mCodeNum = codeNum;
    }

    String convertCodeToInt(String code) {
        BigInteger number = new BigInteger("0");
        BigInteger addFactor = new BigInteger("1");
        char []codes = code.toCharArray();
        for (int i = codes.length - 1; i >= 0; i--) {
            number = number.add(addFactor.multiply(BigInteger.valueOf((int)codes[i] - mCodeBase)));
            addFactor = addFactor.multiply(BigInteger.valueOf(mCodeNum));
        }

        return number.toString();
    }

    String convertIntToCode(String num) {
        int size = 0;
        BigInteger number = new BigInteger(num);
        while(number.compareTo(BigInteger.valueOf(0)) != 0) {
            number = number.divide(BigInteger.valueOf(mCodeNum));
            size++;
        }

        char []codes = new char[size];
        number = new BigInteger(num);
        while(number.compareTo(BigInteger.valueOf(0)) != 0) {
            int remain = number.mod(BigInteger.valueOf(mCodeNum)).intValue();
            number = number.divide(BigInteger.valueOf(mCodeNum));
            codes[--size] = (char)(mCodeBase + remain);
        }

        return String.valueOf(codes);
    }
}