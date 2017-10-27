package seta.noip.me.ppcalculator;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import butterknife.BindInt;
import butterknife.BindView;
import butterknife.ButterKnife;

/** http://www.javahelps.com/2015/03/android-simple-calculator.html
 * this is where the calculator code comes from.
 * I added formula display, intent return value, and qr intent invocation
 */
public class CalculatorActivity extends AppCompatActivity {
    // IDs of all the numeric buttons
    private int[] numericButtons = {R.id.btnZero, R.id.btnOne, R.id.btnTwo, R.id.btnThree, R.id.btnFour, R.id.btnFive, R.id.btnSix, R.id.btnSeven, R.id.btnEight, R.id.btnNine};
    // IDs of all the operator buttons
    private int[] operatorButtons = {R.id.btnAdd, R.id.btnSubtract, R.id.btnMultiply, R.id.btnDivide};
    // TextView used to display the output
    @BindView(R.id.txtFormula) TextView txtFormula;
    @BindView(R.id.txtResult) TextView txtResult;
    @BindInt(R.integer.formula_limit) int formulaLimit;

    // Represent whether the lastly pressed key is numeric or not
    private boolean lastNumeric;
    // Represent that current state is in error or not
    private boolean stateError;
    // If true, do not allow to add another DOT
    private boolean lastDot;
    // if true, last key pressed was equal sign
    private boolean lastEqual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);
        ButterKnife.bind(this);

        // Find and set OnClickListener to numeric buttons
        setNumericOnClickListener();
        // Find and set OnClickListener to operator buttons, equal button and decimal point button
        setOperatorOnClickListener();
    }

    /**
     * Find and set OnClickListener to numeric buttons.
     */
    private void setNumericOnClickListener() {
        // Create a common OnClickListener
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Just append/set the text of clicked button
                Button button = (Button) v;
                if (stateError) {
                    // If current state is Error, replace the error message
                    txtFormula.setText(button.getText());
                    stateError = false;
                } else {
                    // If not, already there is a valid expression so append to it
                    if (txtFormula.length() > formulaLimit) {
                        Animation flasher = new AlphaAnimation(0.0f, 1.0f);
                        flasher.setDuration(50);
                        flasher.setStartOffset(0);
                        flasher.setRepeatMode(Animation.REVERSE);
                        flasher.setRepeatCount(1);
                        txtFormula.startAnimation(flasher);
                    } else {
                        txtFormula.append(button.getText());
                    }
                }
                // Set the flag
                lastNumeric = true;
                lastEqual = false;
            }
        };
        // Assign the listener to all the numeric buttons
        for (int id : numericButtons) {
            findViewById(id).setOnClickListener(listener);
        }
    }

    /**
     * Find and set OnClickListener to operator buttons, equal button and decimal point button.
     */
    private void setOperatorOnClickListener() {
        // Create a common OnClickListener for operators
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If the current state is Error do not append the operator
                // If the last input is number only, append the operator
                if (lastNumeric && !stateError) {
                    Button button = (Button) v;
                    String operator = (String) button.getTag();
                    txtFormula.append(operator);
                    lastNumeric = false;
                    lastDot = false;    // Reset the DOT flag
                    lastEqual = false;
                }
            }
        };
        // Assign the listener to all the operator buttons
        for (int id : operatorButtons) {
            findViewById(id).setOnClickListener(listener);
        }
        // Decimal point
        findViewById(R.id.btnDot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastNumeric && !stateError && !lastDot) {
                    txtFormula.append(".");
                    lastNumeric = false;
                    lastDot = true;
                    lastEqual = false;
                }
            }
        });
        // Clear button
        findViewById(R.id.btnClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtFormula.setText("");  // Clear the screen
                txtResult.setText("");
                // Reset all the states and flags
                lastNumeric = false;
                stateError = false;
                lastDot = false;
                lastEqual = false;
            }
        });
        // Equal button
        findViewById(R.id.btnEqual).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEqual();
            }
        });
    }

    /**
     * Logic to calculate the solution.
     */
    private void onEqual() {
        // If the current state is error, nothing to do.
        // If the last input was equal, and press again, move from result to formula
        if (lastEqual && !stateError) {
            NumberFormat nf = NumberFormat.getInstance();
            BigDecimal ret = BigDecimal.ZERO;
            if (nf instanceof DecimalFormat) {
                ((DecimalFormat) nf).setGroupingUsed(true);
                ((DecimalFormat) nf).setParseBigDecimal(true);
                try {
                    ret = (BigDecimal) nf.parse(txtResult.getText().toString());
                } catch (ParseException e) {
                    stateError = true;
                    // convert to runtime error
                    throw new RuntimeException(txtResult.getText().toString(), e);
                }
            }
            txtFormula.setText(ret.toString());
            txtResult.setText("");
            // treat as numeric input using txtResult
            lastNumeric = true;
            stateError = false;
            lastDot = true;
        }
        // If the current state is error, nothing to do.
        // If the last input is a number only, solution can be found.
        else if (lastNumeric && !stateError) {
            // Read the expression
            String txt = txtFormula.getText().toString();
            // Create an Expression (A class from exp4j library)
            Expression expression = new ExpressionBuilder(txt).build();
            try {//99*6.5*66.38
                // Calculate the result and display
                double result = expression.evaluate();
                BigDecimal ret = new BigDecimal(result);
                ret = ret.setScale(2, RoundingMode.HALF_UP);
                NumberFormat nf = NumberFormat.getInstance();
                nf.setGroupingUsed(true);
                txtResult.setText(nf.format(ret));
                lastDot = true; // Result contains a dot
                lastEqual = true;
            } catch (ArithmeticException ex) {
                // Display an error message
                txtResult.setText("Error");
                stateError = true;
                lastNumeric = false;
                lastEqual = false;
            }
        }
    }
}
