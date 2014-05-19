package junit.sorcer.core.dispatch;

import junit.sorcer.core.provider.*;
import org.junit.Assert;
import org.junit.Test;
import sorcer.core.dispatch.ExertionSorter;
import sorcer.core.dispatch.SortingException;
import sorcer.core.provider.jobber.ServiceJobber;
import sorcer.service.Exertion;
import sorcer.service.Job;
import sorcer.service.Strategy;
import sorcer.service.Task;

import java.util.ArrayList;
import java.util.List;

import static sorcer.co.operator.from;
import static sorcer.eo.operator.*;

/**
 * SORCER class
 * User: Pawel Rubach
 * Date: 23.10.13
 */
public class ExertionSorterTest {

    private static void printExertions(List<Exertion> exertions) {
        int i = 0;
        for (Exertion xrt : exertions) {
            System.out.println("Exertion: " + i + " " + xrt.getName());
            i++;
        }
    }

    private static void printAllExertions(Exertion topXrt) {
        if (topXrt.isTask())
            System.out.print("T " + topXrt.getName() + " ");
        else {
            System.out.println("J " + topXrt.getName() + " {");
            for (Exertion xrt : topXrt.getExertions()) {
                printAllExertions(xrt);
            }
            System.out.println(" }");
        }
    }

    // two level job composition with PULL and PAR execution
    private static Job createJob(Strategy.Flow flow, Strategy.Access access) throws Exception {
        Task t3 = task(
                "t3",
                sig("subtract", Subtractor.class),
                context("subtract", in("arg/x1", null), in("arg/x2", null),
                        out("result/y", null)));
        Task t4 = task("t4",
                sig("multiply", Multiplier.class),
                context("multiply", in("arg/x1", 10.0), in("arg/x2", 50.0),
                        out("result/y", null)));
        Task t5 = task("t5",
                sig("add", Adder.class),
                context("add", in("arg/x1", 20.0), in("arg/x2", 80.0),
                        out("result/y", null)));

        // Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
        return job("j1", t3, // sig("service", Jobber.class),
                job("j2", t5, t4, strategy(flow, access)),
                pipe(out(t4, "result/y"), in(t3, "arg/x1")),
                pipe(out(t5, "result/y"), in(t3, "arg/x2")));
    }


    private static Job createSrv() throws Exception {
        Task t3 = srv("t3", sig("subtract", SubtractorImpl.class),
                cxt("subtract", in("arg/x1"), in("arg/x2"),
                        out("result/y")));

        Task t4 = srv("t4", sig("multiply", MultiplierImpl.class),
                //cxt("multiply", in("super/arg/x1"), in("arg/x2", 50.0),
                cxt("multiply", in("arg/x1", 10.0), in("arg/x2", 50.0),
                        out("result/y")));

        Task t5 = srv("t5", sig("add", AdderImpl.class),
                cxt("add", in("arg/x1", 20.0), in("arg/x2", 80.0),
                        out("result/y")));

        // Service Composition j1(j2(t4(x1, x2), t5(x1, x2)), t3(x1, x2))
        //Job j1= job("j1", job("j2", t4, t5, strategy(Flow.PARALLEL, Access.PULL)), t3,
        return srv("j1", sig("execute", ServiceJobber.class),
                cxt(in("arg/x1", 10.0), result("job/result", from("j1/t3/result/y"))),
                srv("j2", sig("execute", ServiceJobber.class), t4, t5),
                t3,
                pipe(out(t4, "result/y"), in(t3, "arg/x1")),
                pipe(out(t5, "result/y"), in(t3, "arg/x2")));
    }


    private static Job createComplexJob() throws Exception {

        Task f4 = task("Task_f4", sig("multiply", Multiplier.class),
                context("multiply", input(path("arg/x1"), 2), input(path("arg/x2"), 25 * 2),
                        out(path("result/y1"), null)), strategy(Strategy.Access.PUSH, Strategy.Flow.SEQ, Strategy.Monitor.NOTIFY_ALL, Strategy.Provision.TRUE, Strategy.Wait.TRUE));

        Task f44 = task("Task_f44", sig("multiply", Multiplier.class),
                context("multiply", input(path("arg/x41"), 10.0d), input(path("arg/x42"), 50.0d),
                        out(path("result/y41"), null)));

        Task f5 = task("Task_f5", sig("add", Adder.class),
                context("add", input(path("arg/x3"), 20.0d), input(path("arg/x4"), 80.0d),
                        output(path("result/y2"), null)));

        Task f6 = task("Task_f6", sig("multiply", Multiplier.class),
                context("multiply", input(path("arg/x7"), 11.0d), input(path("arg/x8"), 51.0d),
                        out(path("result/y4"), null)));

        Task f7 = task("Task_f7", sig("multiply", Multiplier.class),
                context("multiply", input(path("arg/x9"), 12.0d), input(path("arg/x10"), 52.0d),
                        out(path("result/y5"), null)));

        Task f9 = task("Task_f9", sig("multiply", Multiplier.class),
                context("multiply", input(path("arg/x11"), 13.0d), input(path("arg/x12"), 53.0d),
                        out(path("result/y6"), null)));

        Task f10 = task("Task_f10", sig("multiply", Multiplier.class),
                context("multiply", input(path("arg/x13"), 14.0d), input(path("arg/x14"), 54.0d),
                        out(path("result/y7"), null)));

        Task f3 = task("Task_f3", sig("subtract", Subtractor.class),
                context("subtract", input(path("arg/x5"), null), input(path("arg/x6"), null),
                        output(path("result/y3"), null)));

        Task f55 = task("Task_f55", sig("add", Adder.class),
                context("add", input(path("arg/x53"), 20.0d), input(path("arg/x54"), 80.0d), output(path("result/y52"), null)));

        Task f21 = task("Task_f21", sig("multiply", Multiplier.class), context("Task_f21", input(path("arg2"), 50.5d), input(path("arg1"), 20.0d), output(path("fillMeOut"), null)), strategy(Strategy.Access.PUSH, Strategy.Flow.SEQ, Strategy.Monitor.NOTIFY_ALL, Strategy.Provision.FALSE, Strategy.Wait.TRUE));

        Task f22 = task("Task_f22", sig("add", Adder.class), context("Task_f22", input(path("arg4"), 23d), input(path("arg3"), 43d), output(path("fillMeOut"), null)), strategy(Strategy.Access.PUSH, Strategy.Flow.SEQ, Strategy.Monitor.NOTIFY_ALL, Strategy.Provision.FALSE, Strategy.Wait.TRUE));

        Job f20 = job("Job_f20", f22 , f21 );

        Job j8 = job("Job_f8", pipe(out(f10, path("result/y7")), input(f55, path("arg/x54"))), pipe(out(f7, path("result/y5")), input(f55, path("arg/x53"))), f55, f10, f9,
                pipe(out(f9, path("result/y6")), input(f10, path("arg/x13"))));

        Pipe p1 = pipe(out(f4, path("result/y1")), input(f7, path("arg/x9")));

        return job("Job_f1", f3, j8, f20, job("Job_f2", f5, f7, f6, f4),
                pipe(out(f6, path("result/y4")), input(f5, path("arg/x3"))),
                pipe(out(f4, path("result/y1")), input(f3, path("arg/x5"))),
                pipe(out(f5, path("result/y2")), input(f3, path("arg/x6"))), p1);
    }

    @Test
    public void testSorterSimple() throws Exception {
        System.out.println("Before sorting");
        Job job = createSrv(); //createComplexJob();
        printAllExertions(job);
        ExertionSorter es = new ExertionSorter(job);
        System.out.println("After sorting");
        printAllExertions(es.getSortedJob());
        Assert.assertEquals(Strategy.Flow.SEQ, es.getSortedJob().getFlowType());
        Assert.assertEquals(Strategy.Flow.PAR, es.getSortedJob().getExertion("j2").getFlowType());

    }

    @Test
    public void testSorterSimple2() throws Exception {
        System.out.println("Before sorting");
        Job job = createJob(Strategy.Flow.AUTO, Strategy.Access.PULL);
        printAllExertions(job);
        ExertionSorter es = new ExertionSorter(job);
        System.out.println("After sorting");
        printAllExertions(es.getSortedJob());
        Assert.assertEquals(Strategy.Flow.PAR, es.getSortedJob().getExertion("j2").getFlowType());
    }

    @Test
    public void testSorterComplex() throws Exception {
        System.out.println("Before sorting");
        Job job = createComplexJob();
        printAllExertions(job);
        ExertionSorter es = new ExertionSorter(job);
        System.out.println("After sorting");
        printAllExertions(es.getSortedJob());
        final Exertion f3 = job.getExertion("Task_f3");
        final Exertion j2 = job.getExertion("Job_f2");
        final Exertion j8 = job.getExertion("Job_f8");
        final Exertion j20 = job.getExertion("Job_f20");
        List<Exertion> expList = new ArrayList<Exertion>();
        expList.add(j2);
        expList.add(f3);
        expList.add(j8);
        expList.add(j20);
        Assert.assertArrayEquals(expList.toArray(), es.getSortedJob().getExertions().toArray());
    }

}
