package ru.javaprojects.doccopymaker.core.pathmaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.javaprojects.doccopymaker.core.properties.DocSpecifiers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class DocumentPathMaker {
    private static final String VUIA_COMPANY_CODE = "ВУИА";
    private static final String _UPI_A_COMPANY_CODE = "ЮПИЯ";
    private static final String BA_COMPANY_CODE = "БА";
    private static final String VUIA = "VUIA";
    private static final String _UPI_A = "_UPI_A";
    private static final String BA = "BA";
    private static final String DOT = ".";
    private static final String SPECIFICATION_SPECIFIER = "СП";
    private static final String DETAIL_SPECIFIER = "КД";
    private static final String LETTER_REPLACEMENT_CHARACTER = "|";
    private final Logger log = LoggerFactory.getLogger(getClass());

    public Path makePath(String decimalNumber) {
        decimalNumber = prepareCompanyCode(decimalNumber);
        String companyCode = decimalNumber.substring(0, decimalNumber.indexOf(DOT));
        String middlePart;
        String lastPartPath;
        if (isSoftware(decimalNumber, companyCode)) {
            middlePart = getSoftwareDecNumberMiddlePart(decimalNumber, companyCode);
            String lastPart = decimalNumber.replace(companyCode + DOT + middlePart, "");
            lastPartPath = makeSoftwareDecNumberLastPartPath(lastPart);
        } else {
            middlePart = getDecNumberMiddlePart(decimalNumber, companyCode);
            String lastPart = decimalNumber.replace(companyCode + DOT + middlePart + DOT, "");
            lastPart = checkSpOrDetail(middlePart, lastPart);
            lastPartPath = makeDecNumberLastPartPath(lastPart);
        }
        return Paths.get(companyCode + "/" + middlePart + "/" + lastPartPath);
    }

    private String prepareCompanyCode(String decimalNumber) {
        if (decimalNumber.startsWith(VUIA_COMPANY_CODE + DOT)) {
            return decimalNumber.replace(VUIA_COMPANY_CODE, VUIA);
        } else if (decimalNumber.startsWith(_UPI_A_COMPANY_CODE + DOT)) {
            return decimalNumber.replace(_UPI_A_COMPANY_CODE, _UPI_A);
        } else if (decimalNumber.startsWith(BA_COMPANY_CODE) && Character.toString(decimalNumber.charAt(3)).equals(DOT)) {
            decimalNumber = decimalNumber.replace(BA_COMPANY_CODE, "").replaceFirst("\\.", "");
            return BA + DOT + decimalNumber;
        } else {
            String message = "Unsupported company code in decimal number:" + decimalNumber;
            log.warn(message);
            throw new UnsupportedCompanyCodeException(message);
        }
    }

    private boolean isSoftware(String decimalNumber, String companyCode) {
        boolean isSoftware = false;
        if (companyCode.equals(VUIA) || companyCode.equals(_UPI_A)) {
            isSoftware = decimalNumber.replace(companyCode + DOT, "").matches("[0-2].*");
        }
        return isSoftware;
    }

    private String getDecNumberMiddlePart(String decimalNumber, String companyCode) {
        String trimCompanyCode = decimalNumber.replace(companyCode + DOT, "");
        if (trimCompanyCode.contains(DOT)) {
            return trimCompanyCode.substring(0, trimCompanyCode.indexOf(DOT));
        } else {
            String message = "Unsupported decimal number type:" + decimalNumber;
            log.warn(message);
            throw new UnsupportedDecimalNumberTypeException(message);
        }
    }

    private String getSoftwareDecNumberMiddlePart(String decimalNumber, String companyCode) {
        String trimCompanyCode = decimalNumber.replace(companyCode + DOT, "");
        if (trimCompanyCode.contains("-")) {
            return trimCompanyCode.substring(0, trimCompanyCode.indexOf("-"));
        } else {
            String message = "Unsupported software decimal number type:" + decimalNumber;
            log.warn(message);
            throw new UnsupportedDecimalNumberTypeException(message);
        }
    }

    private String checkSpOrDetail(String middlePart, String lastPart) {
        if (lastPart.matches("[^А-Яа-я]+")) {
            if (middlePart.matches("[1-6].*")) {
                return lastPart + SPECIFICATION_SPECIFIER;
            } else if (middlePart.matches("[7-9].*")) {
                return lastPart + DETAIL_SPECIFIER;
            }
        }
        return lastPart;
    }

    private String makeDecNumberLastPartPath(String lastPart) {
        lastPart = lastPart.replace(" ", "");
        int firstLetterIndex = lastPart.replaceFirst("[А-Яа-я]", LETTER_REPLACEMENT_CHARACTER).indexOf(LETTER_REPLACEMENT_CHARACTER);
        String numbersPart = lastPart.substring(0, firstLetterIndex);
        String docSpecifierPart = lastPart.substring(firstLetterIndex);
        String docSpecifier = DocSpecifiers.getDocSpecifier(docSpecifierPart);
        if (Objects.isNull(docSpecifier)) {
            String message = "Unsupported document specifier:" + docSpecifierPart;
            log.warn(message);
            throw new UnsupportedDocSpecifierException(message);
        }
        if (numbersPart.contains("-")) {
            int dashIndex = numbersPart.indexOf("-");
            numbersPart = numbersPart.substring(0, dashIndex) + "/" + numbersPart.substring(dashIndex);
        }
       return  (numbersPart + "/" + docSpecifier);
    }

    private String makeSoftwareDecNumberLastPartPath(String lastPart) {
        if (lastPart.trim().length() == 3) {
            return lastPart + "/" + DocSpecifiers.getDocSpecifier(SPECIFICATION_SPECIFIER);
        } else {
            String[] lastPartParts = lastPart.split(" ");
            StringBuilder lastPartPath = new StringBuilder();
            for (String part : lastPartParts) {
                lastPartPath.append(part).append("/");
            }
            return lastPartPath.toString();
        }
    }
}