package org.rapla.plugin.wwi2021;


import org.rapla.entities.IllegalAnnotationException;
import org.rapla.entities.User;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Classifiable;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.internal.EvalContext;
import org.rapla.entities.extensionpoints.Function;
import org.rapla.entities.extensionpoints.FunctionFactory;
import org.rapla.inject.Extension;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

@Extension(provides = FunctionFactory.class, id=showCommentFunctions.NAMESPACE)


public class showCommentFunctions implements FunctionFactory{


    static final public String NAMESPACE = "org.rapla.showcomment";

    public @Inject showCommentFunctions(){

    }


    @Override public Function createFunction(String functionName, List<Function> args) throws IllegalAnnotationException
    {
        if ( functionName.equals(CommentFunction.name))
        {
            return new CommentFunction(args);
        }
        return null;
    }

    private String showComment(Object obj)
    {
        final String l;

        if ( obj instanceof AppointmentBlock)
        {
            Appointment appointment = ((AppointmentBlock)obj).getAppointment();
            String comment = appointment.getComment();
            if (comment == null)
            {
                l = "";
            }
            else
            {
                l = "\n" + "(" + comment + ")";
            }
        }
        else if (obj instanceof Appointment)
        {
            Appointment appointment = (Appointment)obj;
            String comment = appointment.getComment();
            if (comment == null)
            {
                l = "";
            }
            else
            {
                l = "\n" + "(" + comment + ")";
            }
        }
        else if (obj instanceof Reservation)
        {
            l = "";
        }
        else if ( obj instanceof Collection)
        {
            l = "";
        }
        else
        {
            l = "";
        }
        return l;
    }

    class CommentFunction extends Function
    {
        static public final String name = "comment";
        Function arg;

        public CommentFunction( List<Function> args) throws IllegalAnnotationException
        {
            super(NAMESPACE,name, args);
            assertArgs(0,1);
            if ( args.size() > 0)
            {
                arg = args.get( 0);
            }

        }

        @Override public String eval(EvalContext context)
        {
            final Object obj;
            if ( arg != null)
            {
                obj = arg.eval( context);
            }
            else
            {
                obj = context.getFirstContextObject();
            }
            final String result = showComment(obj);
            return result;
        }

    }

}



