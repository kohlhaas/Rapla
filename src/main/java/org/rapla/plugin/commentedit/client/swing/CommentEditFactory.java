package org.rapla.plugin.commentedit.client.swing;

import org.rapla.RaplaResources;
import org.rapla.client.RaplaWidget;
import org.rapla.client.extensionpoints.CommentExtensionFactory;
import org.rapla.client.extensionpoints.AppointmentStatusFactory;
import org.rapla.client.extensionpoints.CommentExtensionFactory;
import org.rapla.entities.domain.Appointment;
import org.rapla.facade.client.ClientFacade;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.inject.Extension;
import org.rapla.logger.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.function.Consumer;

@Extension (provides = CommentExtensionFactory.class, id = "commentextension")
@Singleton
public class CommentEditFactory implements CommentExtensionFactory {
    private final ClientFacade facade;
    private final RaplaResources i18n;
    private final RaplaLocale raplaLocale;
    private final Logger logger;

    @Inject
    public CommentEditFactory(ClientFacade facade, RaplaResources i18n, RaplaLocale raplaLocale, Logger logger)
    {
        super();
        this.facade = facade;
        this.i18n = i18n;
        this.raplaLocale = raplaLocale;
        this.logger = logger;
    }

    @Override
    public RaplaWidget createComment(AppointmentEditExtensionEvents events) throws RaplaException {
        return new CommentEditFactory.CommentEditor(events);
    }

    class CommentEditor implements RaplaWidget, Consumer<Appointment>
    {
        JTextField commentField = new JTextField();
        AppointmentEditExtensionEvents events;
        CommentEditor(AppointmentEditExtensionEvents events)
        {
            this.events = events;
            events.init(this);
            commentField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    events.appointmentChanged();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    events.appointmentChanged();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    Appointment appointment = events.getAppointment();
                    appointment.setComment(commentField.getText());
                    events.appointmentChanged();
                }
            });
        }

        public JComponent getComponent()
        {
            return commentField;
        }

        @Override
        public void accept(Appointment appointment) {
            appointment.getStart().getTime();
            commentField.setText(appointment.getComment());
        }
    }
    
}
