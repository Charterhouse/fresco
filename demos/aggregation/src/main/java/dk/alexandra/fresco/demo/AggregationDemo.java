package dk.alexandra.fresco.demo;

import dk.alexandra.fresco.demo.EncryptAndRevealStep.RowWithCipher;
import dk.alexandra.fresco.demo.helpers.ResourcePoolHelper;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.ProtocolEvaluator;
import dk.alexandra.fresco.framework.builder.ProtocolBuilderNumeric.SequentialNumericBuilder;
import dk.alexandra.fresco.framework.network.NetworkingStrategy;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.sce.configuration.SCEConfiguration;
import dk.alexandra.fresco.framework.sce.evaluator.SequentialEvaluator;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.framework.sce.resources.storage.StreamedStorage;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.suite.ProtocolSuite;
import dk.alexandra.fresco.suite.spdz.SpdzProtocolSuite;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import dk.alexandra.fresco.suite.spdz.configuration.PreprocessingStrategy;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class AggregationDemo<ResourcePoolT extends ResourcePool> {

  public AggregationDemo() {}

  /**
   * @return Generates mock input data.
   */
  public List<List<BigInteger>> readInputs() {
    return Arrays.asList(Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(10)),
        Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(7)),
        Arrays.asList(BigInteger.valueOf(2), BigInteger.valueOf(100)),
        Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(50)),
        Arrays.asList(BigInteger.valueOf(3), BigInteger.valueOf(15)),
        Arrays.asList(BigInteger.valueOf(3), BigInteger.valueOf(15)),
        Arrays.asList(BigInteger.valueOf(2), BigInteger.valueOf(70)));
  }

  /**
   * @param result Prints result values to console.
   */
  public void writeOutputs(List<List<BigInteger>> result) {
    for (List<BigInteger> row : result) {
      for (BigInteger value : row) {
        System.out.print(value + " ");
      }
      System.out.println();
    }
  }

  /**
   * @return Uses deterministic encryption (in this case MiMC) for encrypt, under MPC, the values in
   *         the specified column, and opens the resulting cipher texts. The resulting OInts are
   *         appended to the end of each row.
   *
   *         NOTE: This leaks the equality of the encrypted input values.
   *
   *         Example: ([k], [v]) -> ([k], [v], enc(k)) for columnIndex = 0
   */
  public List<RowWithCipher> encryptAndReveal(SCEConfiguration<ResourcePoolT> sceConf,
      SecureComputationEngine<ResourcePoolT, SequentialNumericBuilder> sce,
      List<List<SInt>> inputRows, int columnIndex, ResourcePoolT rp) throws IOException {
    EncryptAndRevealStep ear = new EncryptAndRevealStep(inputRows, columnIndex);
    return sce.runApplication(ear, rp);
  }

  /**
   * @return Takes in a secret-shared collection of rows (2d-array) and returns the secret-shared
   *         result of a sum aggregation of the values in the agg column grouped by the values in
   *         the key column.
   *
   *         This method invokes encryptAndReveal and the aggregate step.
   *
   *         Example: ([1], [2]), ([1], [3]), ([2], [4]) -> ([1], [5]), ([2], [4]) for keyColumn = 0
   *         and aggColumn = 1
   */
  public List<List<SInt>> aggregate(SCEConfiguration<ResourcePoolT> sceConf,
      SecureComputationEngine<ResourcePoolT, SequentialNumericBuilder> sce, ResourcePoolT rp,
      List<List<SInt>> inputRows, int keyColumn, int aggColumn) throws IOException {
    // TODO: need to shuffle input rows and result
    List<RowWithCipher> rowsWithOpenenedCiphers =
        encryptAndReveal(sceConf, sce, inputRows, keyColumn, rp);
    AggregateStep aggStep = new AggregateStep(rowsWithOpenenedCiphers, keyColumn, aggColumn);
    return sce.runApplication(aggStep, rp);
  }

  /**
   * @return Runs the input step which secret shares all int values in inputRows. Returns and SInt
   *         array containing the resulting shares.
   */
  public List<List<SInt>> secretShare(SCEConfiguration<ResourcePoolT> sceConf,
      SecureComputationEngine<ResourcePoolT, SequentialNumericBuilder> sce,
      List<List<BigInteger>> inputRows, int pid, ResourcePoolT rp) throws IOException {
    InputStep inputStep = new InputStep(inputRows, pid);
    return sce.runApplication(inputStep, rp);
  }

  /**
   * @return Runs the output step which opens all secret shares.
   */
  public List<List<BigInteger>> open(SCEConfiguration<ResourcePoolT> sceConf,
      SecureComputationEngine<ResourcePoolT, SequentialNumericBuilder> sce,
      List<List<SInt>> secretShares, ResourcePoolT rp) throws IOException {
    OutputStep outputStep = new OutputStep(secretShares);
    return sce.runApplication(outputStep, rp);
  }

  public void runApplication(SCEConfiguration<ResourcePoolT> sceConf,
      SecureComputationEngine<ResourcePoolT, SequentialNumericBuilder> sce, ResourcePoolT rp)
          throws IOException {
    int pid = sceConf.getMyId();
    int keyColumnIndex = 0;
    int aggColumnIndex = 1;

    // Read inputs. For now this just returns a hard-coded array of values.
    List<List<BigInteger>> inputRows = readInputs();

    // Secret-share the inputs.
    List<List<SInt>> secretSharedRows = secretShare(sceConf, sce, inputRows, pid, rp);

    // Aggregate
    List<List<SInt>> aggregated =
        aggregate(sceConf, sce, rp, secretSharedRows, keyColumnIndex, aggColumnIndex);

    // Recombine the secret shares of the result
    List<List<BigInteger>> openedResult = open(sceConf, sce, aggregated, rp);

    // Write outputs. For now this just prints the results to the console.
    writeOutputs(openedResult);

    sce.shutdownSCE();
  }

  public static void main(String[] args) throws IOException {

    // My player ID
    int myPID = Integer.parseInt(args[0]);

    // Set up our SecureComputationEngine configuration
    SCEConfiguration<SpdzResourcePool> sceConfig = new SCEConfiguration<SpdzResourcePool>() {

      @Override
      public int getMyId() {
        return myPID;
      }

      @Override
      public Map<Integer, Party> getParties() {
        // Set up network details of our two players
        Map<Integer, Party> parties = new HashMap<>();
        parties.put(1, new Party(1, "localhost", 8001));
        parties.put(2, new Party(2, "localhost", 8002));
        return parties;
      }

      @Override
      public Level getLogLevel() {
        return Level.INFO;
      }

      @Override
      public ProtocolEvaluator<SpdzResourcePool> getEvaluator() {
        // We will use a sequential evaluation strategy
        SequentialEvaluator<SpdzResourcePool> sequentialEvaluator = new SequentialEvaluator<>();
        sequentialEvaluator.setMaxBatchSize(4096);
        return sequentialEvaluator;
      }

      @Override
      public StreamedStorage getStreamedStorage() {
        // We will not use StreamedStorage
        return null;
      }

      @Override
      public NetworkingStrategy getNetworkStrategy() {
        return NetworkingStrategy.KRYONET;
      }

    };

    ProtocolSuite<SpdzResourcePool, SequentialNumericBuilder> suite =
        new SpdzProtocolSuite(150, PreprocessingStrategy.DUMMY, null);
    // Instantiate environment
    SecureComputationEngine<SpdzResourcePool, SequentialNumericBuilder> sce =
        new SecureComputationEngineImpl<SpdzResourcePool, SequentialNumericBuilder>(suite,
            sceConfig.getEvaluator(), sceConfig.getLogLevel());

    // Create application we are going run
    AggregationDemo<SpdzResourcePool> app = new AggregationDemo<>();

    SpdzResourcePool rp = ResourcePoolHelper.createResourcePool(sceConfig, suite);
    app.runApplication(sceConfig, sce, rp);
  }
}
