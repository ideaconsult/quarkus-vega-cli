package net.idea.wrapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import insilico.core.model.iInsilicoModel;
import insilico.core.model.InsilicoModelOutput;
import insilico.core.molecule.InsilicoMolecule;
import insilico.core.localization.StringSelectorCore;
import insilico.core.tools.utils.ModelUtilities;
import insilico.core.version.InsilicoInfo;
import insilico.core.ad.item.iADIndex;
import insilico.core.exception.InitFailureException;


public class InsilicoModelOutputWriter {
    private final BufferedWriter writer;

    public InsilicoModelOutputWriter(BufferedWriter writer) {
        this.writer = writer;
    }

    public void writeHeader(iInsilicoModel model) throws IOException {
        StringBuilder header = new StringBuilder();
        String var10001 = StringSelectorCore.getString("report_txt_intro");
        header.append(var10001 + System.lineSeparator());
        var10001 = String.format(StringSelectorCore.getString("report_txt_version"), model.getInfo().getName(), model.getInfo().getVersion());
        header.append(var10001 + System.lineSeparator());

        try {
            InsilicoInfo icv = new InsilicoInfo();
            var10001 = String.format(StringSelectorCore.getString("report_txt_core_version"), icv.getVersion());
            header.append(var10001 + System.lineSeparator());
        } catch (InitFailureException var10) {
            header.append(System.lineSeparator());
            System.err.println(String.format(StringSelectorCore.getString("report_txt_init_exception"), var10.getMessage()));
        }
        header.append(System.lineSeparator());
        System.out.println(System.lineSeparator() +header.toString());
        header.append("No.\tID\tSMILES");  // initial fixed columns

        // Add model result names
        for (String name : model.GetResultsName()) {
            header.append("\t").append(name);
        }

        header.append("\t").append(StringSelectorCore.getString("report_txt_experimental"));
        
        if (model.GetTrainingSet().hasUnits()) {
            header.append(" [").append(model.GetTrainingSet().getUnits()).append("]");
        }

        if (model.getInfo().hasAlerts()) {
            header.append("\t").append(StringSelectorCore.getString("report_txt_struct_alerts"));
        }

        header.append("\t").append(StringSelectorCore.getString("report_txt_adi"));

        for (String adiName : model.GetADItemsName()) {
            header.append("\t").append(adiName);
        }

        header.append("\t").append(StringSelectorCore.getString("report_txt_struct_remarks"));
        writer.write(header.toString());
        writer.newLine();
        writer.flush();
    }

    /**
     * Write a single InsilicoModelOutput using the provided iInsilicoModel.
     */
    public void writeOne(int index, InsilicoModelOutput output, InsilicoMolecule molecule, iInsilicoModel model) throws IOException {
        StringBuilder line = new StringBuilder();

        line.append(index).append("\t");
        line.append(output.getMoleculeId()).append("\t");
        line.append(output.getMoleculeSMILES());

        if (output.getStatus() < 1) {
            line.append("\t[").append(StringSelectorCore.getString("report_txt_struct_error")).append("]");
            int nCols = model.GetResultsName().length + 1 + (model.getInfo().hasAlerts() ? 1 : 0) + 1 + model.GetADItemsName().length;
            for (int j = 0; j < nCols; j++) {
                line.append("\t-");
            }
        } else {
            line.append("\t").append(output.getAssessment());

            for (String res : output.getResults()) {
                line.append("\t").append(res);
            }

            line.append("\t").append(output.getExperimentalFormatted());

            if (model.getInfo().hasAlerts()) {
                line.append("\t").append(ModelUtilities.BuildSANameList(output.getSAList().getSAList()));
            }

            if (output.getStatus() != 2) {
                line.append("\t").append(output.getADI().GetIndexValueFormatted());
                Iterator<iADIndex> adIndexIter = output.getADIndex().iterator();
                while (adIndexIter.hasNext()) {
                    line.append("\t").append(adIndexIter.next().GetIndexValueFormatted());
                }
            } else {
                int nCols = 1 + model.GetADItemsName().length;
                for (int j = 0; j < nCols; j++) {
                    line.append("\t-");
                }
            }
        }

        // Append warnings, errors, messages
        StringBuilder msg = new StringBuilder();
        for (int w = 0; w < molecule.GetWarnings().GetSize(); w++) {
            msg.append("[").append(StringSelectorCore.getString("report_txt_molecule_warning")).append("]")
               .append(molecule.GetWarnings().GetMessages(w)).append(". ");
        }
        for (int e = 0; e < molecule.GetErrors().GetSize(); e++) {
            msg.append("[").append(StringSelectorCore.getString("report_txt_molecule_error")).append("]")
               .append(molecule.GetErrors().GetMessages(e)).append(". ");
        }
        if (!output.getErrMessage().isEmpty()) {
            msg.append("[").append(StringSelectorCore.getString("report_txt_model")).append("]")
               .append(output.getErrMessage()).append(".");
        }
        if (msg.length() == 0) {
            msg.append("-");
        }

        line.append("\t").append(msg);

        writer.write(line.toString());
        writer.newLine();
        writer.flush();
    }

    /**
     * Close the underlying writer.
     */
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}
