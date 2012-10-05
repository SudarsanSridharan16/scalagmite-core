# Scalagmite 0.1-SNAPSHOT

## About

Scalagmite is a project that aims to ease the writing of scalable parallel
or distributed applications by using high-level abstractions. Threads,
semaphores, etc. are never handled explicitly. Instead, agents are defined
which handle different message types and interact with other agents by
asynchronously sending them messages.

## Example

As an example, let an application be the simulation of *N* simultaneous
ping-pong matches i.e. there are *N* pingers and *N* pongers, each player
sending the ball *M* times.

Let us first define a class holding the parameters of the simulation:

```java
public interface Configuration {
  public static final int M = 20;
  public static final int N = 20;
}
```

Pingers and pongers might be described by Scalagmite agents, the balls exchanged
by the players being messages.

Pinger agent code is as follows:

```java
import scalagmite.AgentException;
import scalagmite.MessageHandler;
import scalagmite.SchedulableAgent;
import scalagmite.events.InitAgent;

public class Pinger extends SchedulableAgent {
  int counter = 0;

  public Pinger() throws AgentException {
    super();

    registerInitHandler(new MessageHandler<InitAgent>() {
      @Override
      public void handle(InitAgent message) throws Exception {
        ping(new Ball());
      }
    });

    registerHandler(Ball.class, new MessageHandler<Ball>() {
      @Override
      public void handle(Ball ball) throws Exception {
        ping(ball);
      }
    });
  }

  private void ping(Ball ball) throws AgentException {
    ++counter;
    System.out.println("Pinger #" + getNumber() + ": ping #" + counter);
    this.route("Ponger", getNumber(), ball);

    if (counter == Configuration.M) {
      System.out.println("Pinger closing...");
      this.stop();
    }
  }
}
```


``Pinger`` extends ``SchedulableAgent`` which means it will be executed by a
thread part of a pool. This way, the number of threads actually used to execute
a simulation is potentially much lower than *N* when *N* becomes big.
``counter`` is the number
of times the ball has been sent to other player. When ``counter`` reaches
*M* (here ``Configuration.M``), the agent stops its execution.
As ``Pinger`` starts the match, it must implement a special handler ("Init
handler") that sends the first ball of a match when invoked.

``Ball`` class represents the ball exchanged between 2 players, in this case
it is simply an empty class. Note that ``Ball`` is instantiated by Pinger as
it always starts the match.

```java
public class Ball {
  public Ball() {
  }
}
```

``Ponger`` is implemented roughly the same way as ``Pinger``, the
differences come from the fact that ``Pinger`` starts the match and
``Ponger`` "ends" it. A consequence is that "Init handler" does not need to
be set.

```java
import scalagmite.AgentException;
import scalagmite.MessageHandler;
import scalagmite.SchedulableAgent;

public class Ponger extends SchedulableAgent {
  int counter;

  public Ponger() throws AgentException {
    super();

    registerHandler(Ball.class, new MessageHandler<Ball>() {
      @Override
      public void handle(Ball ball) throws Exception {
        pong(ball);
      }
    });
  }

  private void pong(Ball ball) throws AgentException {
    ++counter;

    if (counter == Configuration.N) {
      System.out.println("Ponger closing...");
      this.stop();
    } else {
      System.out.println("Ponger #" + getNumber() + ": pong #" + counter);
      this.route("Pinger", getNumber(), ball);
    }
  }
}
```

With these 2 agents and 1 message being defined, the simulation described
above is simply implemented as follows:

```java
import scalagmite.Scalagmite;
import scalagmite.ScalagmiteException;

public class Main {
  public static void main(String[] args) throws ScalagmiteException,
      InterruptedException {
    Scalagmite scalag = Scalagmite.init();
    scalag.register(Pinger.class, Configuration.N);
    scalag.register(Ponger.class, Configuration.N);
    scalag.main();
  }
}
```

where ``Configuration.N`` is an integer constant equal to *N*.
