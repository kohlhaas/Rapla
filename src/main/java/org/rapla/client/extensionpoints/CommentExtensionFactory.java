package org.rapla.client.extensionpoints;

import org.rapla.client.RaplaWidget;
import org.rapla.client.ReservationEdit;
import org.rapla.entities.domain.Appointment;
import org.rapla.framework.RaplaException;
import org.rapla.inject.ExtensionPoint;
import org.rapla.inject.InjectionContext;

import java.util.function.Consumer;

/** add a footer for summary of appointments in edit window
 * provide an AppointmentStatusFactory to add your own footer to the appointment edit
 @see AppointmentStatusFactory
  * */

@ExtensionPoint(context = InjectionContext.swing,id = CommentExtensionFactory.ID)
public interface CommentExtensionFactory {
    String ID = "commentextension";
    RaplaWidget createComment(AppointmentEditExtensionEvents events) throws RaplaException;

    interface AppointmentEditExtensionEvents
    {
        void init(Consumer<Appointment> appointmentChanged);
        Appointment getAppointment();
        void appointmentChanged();
    }
    
}

