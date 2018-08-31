package ar.com.profebot.service;

import android.content.Context;
import android.widget.Toast;

import com.profebot.activities.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ar.com.profebot.Models.MultipleChoiceStep;
import ar.com.profebot.activities.SolvePolynomialActivity;
import ar.com.profebot.intelligent.module.IAModuleClient;
import ar.com.profebot.parser.container.Tree;
import ar.com.profebot.parser.exception.InvalidExpressionException;
import ar.com.profebot.parser.service.ParserService;
import de.uni_bielefeld.cebitec.mzurowie.pretty_formula.main.FormulaParser;

public class FactoringManager {

    // (exponente, coeficiente)
    public static Map<Integer, Integer> polynomialTerms;
    // (raíz, multiplicidad)
    public static Map<Double, Integer> rootsMultiplicity;
    public static List<Double> roots;
    public static String rootsFactorized;
    public static String pendingPolynomial;
    public static Double currentRoot1;
    public static Double currentRoot2;
    public static String currentRootType;
    public static Boolean end;
    private static SolvePolynomialActivity context;

    public static void setContext(SolvePolynomialActivity context) {
        FactoringManager.context = context;
    }

    public static Map<Integer, Integer> getPolynomialTerms() {
        return polynomialTerms;
    }

    public static void setPolynomialTerms(Map<Integer, Integer> polynomialTerms) {
        FactoringManager.polynomialTerms = polynomialTerms;
        roots = new ArrayList<>();
        rootsMultiplicity = new HashMap<>();
        end = false;
    }

    public static MultipleChoiceStep nextStep(){
        Boolean factorComunIsPossible = true;
        Boolean cuadraticIsPossible = true;

        // Veo qué casos son posibles

        for(Integer exponent : polynomialTerms.keySet()){
            if(exponent <= 1){
                factorComunIsPossible = false;
                break;
            }
        }

        Boolean anyExponentIs2 = false;
        Boolean firstTime = true;
        for(Integer exponent : polynomialTerms.keySet()){
            if(firstTime && exponent == 2){
                anyExponentIs2 = true;
                firstTime = false;
            }

            if(exponent > 2){
                cuadraticIsPossible = false;
                break;
            }
        }
        cuadraticIsPossible = cuadraticIsPossible && anyExponentIs2;

        // Fomulo opciones correctas, regulares e incorrectas

        Integer correctOption = null;
        Integer regularOption1 = null;
        Integer regularOption2 = null;

        /**
         * factor comun: 1
         * cuadrática: 2
         * gauss: 3
         */

        if(!factorComunIsPossible && !cuadraticIsPossible){
            correctOption = 3;
        }else if (factorComunIsPossible){
            correctOption = 1;
            if(cuadraticIsPossible){
                regularOption1 = 2;
                regularOption2 = 3;
            }else{
                regularOption1 = 3;
            }
        }else if(cuadraticIsPossible){
            correctOption = 2;
            regularOption1 = 3;
        }

        setFactors();

        return new MultipleChoiceStep(getEquation(), "", "", "", "",
                context.getString(R.string.FACTOR_COMUN), "",
                context.getString(R.string.CUADRATICA), "",
                context.getString(R.string.GAUSS), "",
                correctOption, regularOption1, regularOption2, "","",
                "" );
    }

    public static String getEquation(){
        String rootsFactorizedAux = "";
        if(!rootsFactorized.isEmpty()){
            rootsFactorizedAux = rootsFactorizedAux.replace("x", "a_1");
            rootsFactorizedAux = FormulaParser.parseToLatex(rootsFactorizedAux).replace("{a}_{1}", "x");
            rootsFactorizedAux += "*";
        }

        String pendingPolynomialAux = pendingPolynomial;
        pendingPolynomialAux = pendingPolynomialAux.replace("x", "a_1");
        pendingPolynomialAux = FormulaParser.parseToLatex(pendingPolynomialAux).replace("{a}_{1}", "x");

        return rootsFactorizedAux + "\\mathbf{" + pendingPolynomialAux + "}";
    }

    public static void setFactors(){
        List<Integer> exponents = new ArrayList<>(polynomialTerms.keySet());
        Collections.sort(exponents, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2 >= o1 ? 1 : -1;
            }
        });

        StringBuilder stringBuilder = new StringBuilder("");
        Boolean firstTerm = true;
        for(Integer exponent : exponents){
            Integer coefficient = polynomialTerms.get(exponent);
            String operator = "+";
            if(coefficient < 0 || firstTerm){
                operator = "";
            }
            stringBuilder.append(operator);
            stringBuilder.append(polynomialTerms.get(exponent));
            stringBuilder.append("*x^");
            stringBuilder.append(exponent);
            firstTerm = false;
        }

        // Polinomio a factorizar
        String firstSign = "";
        String equation = stringBuilder.toString().replaceAll("x\\^0","").trim();
        if (equation.substring(0,1).matches("-")){
            firstSign = "-";
            equation = equation.substring(1);
        }
        pendingPolynomial = "(" + firstSign + equation + ")";

        // Raíces ya calculadas
        stringBuilder = new StringBuilder("");
        for(int i = 0 ; i < roots.size() ; i++){
            if(roots.get(i) == 0){
                stringBuilder.append("x");
            }else{
                stringBuilder.append("(x-");
                stringBuilder.append(roots.get(i));
                stringBuilder.append(")");
            }

            if(rootsMultiplicity.get(roots.get(i)) > 1){
                stringBuilder.append("^");
                stringBuilder.append(rootsMultiplicity.get(roots.get(i)));
            }

            if(i + 1 < roots.size()){
                stringBuilder.append("*");
            }
        }
        rootsFactorized = stringBuilder.toString();
    }

    public static void factorizeBy(Integer option){
        initializeVariables();

        switch (option){
            case 1:
                applyCommonFactor();
                break;
            case 2:
                applyQuadratic();
                break;
            default:
                applyGauss();
        }
    }

    private static void initializeVariables(){
        currentRoot1 = null;
        currentRoot2 = null;
        currentRootType = "";
    }

    private static void applyCommonFactor(){
        Integer minExponent = Collections.min(polynomialTerms.keySet());

        for(Integer exponent : polynomialTerms.keySet()){
            polynomialTerms.put(exponent - minExponent, polynomialTerms.get(exponent));
            polynomialTerms.remove(exponent);
        }

        roots.add(0.0);
        rootsMultiplicity.put(0.0, minExponent);

        currentRoot1 = 0.0;
        currentRootType = getMultiplicityName(minExponent);
    }

    private static void applyQuadratic(){
        Integer a = polynomialTerms.get(2);
        Integer b = polynomialTerms.get(1);
        Integer c = polynomialTerms.get(0);

        Integer discriminant = b * b - 4 * a * c;

        if(discriminant < 0){
            end = true;
        }else{
            Double root1 = (-1 * b + Math.sqrt(discriminant)) / (2 * a);
            Double root2 = (-1 * b - Math.sqrt(discriminant)) / (2 * a);

            if(root1.equals(root2)){
                currentRoot1 = root1;
                roots.add(root1);
                rootsMultiplicity.put(root1, 2);
            }else{
                currentRoot1 = root1;
                currentRoot2 = root2;
                roots.add(root1);
                rootsMultiplicity.put(root1, 1);
                roots.add(root2);
                rootsMultiplicity.put(root2, 1);
            }
        }
    }

    private static void applyGauss(){

    }

    private static String getMultiplicityName(Integer multiplicity){
        switch (multiplicity){
            case 1:
                return "simple";
            case 2:
                return "doble";
            case 3:
                return "triple";
            case 4:
                return "cuádruple";
            default:
                return "múltiple";
        }
    }

    public static String getMessageOfRightOption(Integer option){
        String answer;
        switch (option){
            case 1:
                answer = "" + context.getText(R.string.FACTOR_COMUN_ES_EL_CORRECTO);
                return  answer
                        .replace("/raiz/", "" + currentRoot1)
                        .replace("/type/", currentRootType);
            case 2:
                if(currentRoot2 == null){
                    answer = "" + context.getText(R.string.CUADRATICA_RAIZ_DOBLE_ES_EL_CORRECTO);
                    return answer.replace("/raiz/", "" + currentRoot1);
                }
                answer = "" + context.getText(R.string.CUADRATICA_RAICES_SIMPLES_ES_EL_CORRECTO);
                return answer
                        .replace("/raiz1/", "" + currentRoot1)
                        .replace("/raiz2/", "" + currentRoot2);
            default:
                answer = "" + context.getText(R.string.GAUSS_ES_EL_CORRECTO);
                return answer.replace("/raiz/", "" + currentRoot1);
        }
    }

    public static String getMessageOfRegularOptions(Integer regularOption1, Integer regularOption2){
        String regularOption1Text = getMessageOfRegularOptionNotChosen(regularOption1);
        String regularOption2Text = getMessageOfRegularOptionNotChosen(regularOption2);

        String answer = "";

        if(regularOption1Text.isEmpty() && regularOption2Text.isEmpty()){
            return answer;
        }

        if(!regularOption1Text.isEmpty()){
            answer += regularOption1Text;
        }

        if(!regularOption2Text.isEmpty()){
            answer += ". " + regularOption1Text;
        }

        return answer;
    }

    public static String getMessageOfRegularOptionNotChosen(Integer regularOption){
        switch (regularOption){
            case 2:
                return "" + context.getText(R.string.CUADRATICA_ERA_POSIBLE);
            default:
                return "" + context.getText(R.string.GAUSS_ERA_POSIBLE);
        }
    }

    public static String getMessageOfRegularOptionChosen(Integer regularOption){
        switch (regularOption){
            case 2:
                return "" + context.getText(R.string.CUADRATICA_ES_POSIBLE_PERO_NO_LO_MEJOR);
            default:
                return "" + context.getText(R.string.GAUSS_ES_POSIBLE_PERO_NO_LO_MEJOR);
        }
    }

    public static String getMessageOfRightOptionNotChosen(Integer correctOption){
        switch (correctOption){
            case 1:
                return "" + context.getText(R.string.FACTOR_COMUN_ERA_EL_CORRECTO);
            case 2:
                return "" + context.getText(R.string.CUADRATICA_ERA_EL_CORRECTO);
            default:
                return "" + context.getText(R.string.GAUSS_ERA_EL_CORRECTO);
        }
    }

    public static String getMessageOfWrongOptionChosen(Integer optionChosen){
        switch (optionChosen){
            case 1:
                return "" + context.getText(R.string.FACTOR_COMUN_NO_ES_CORRECTO);
            default:
                return "" + context.getText(R.string.CUADRATICA_NO_ES_CORRECTO);
        }
    }

    public static String getCaseNameFrom(Integer option){
        switch (option){
            case 1:
                return "" + context.getText(R.string.FACTOR_COMUN);
            case 2:
                return "" + context.getText(R.string.CUADRATICA);
            default:
                return "" + context.getText(R.string.GAUSS);
        }
    }
}
