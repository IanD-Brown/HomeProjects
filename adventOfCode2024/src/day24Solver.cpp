#include "day24Solver.h"

#include <cstdlib>
#include <iostream>
#include <map>
#include <regex>
#include <set>
#include "solver.h"
#include <string>
#include <vector>

using namespace std;

template <typename S>
ostream &operator<<(ostream &os, const vector<S> &vector) {
  // Printing all the elements using <<
  for (const auto &i : vector) {
    os << toString(i) << ' ';
  }
  return os;
}

enum WireState { FALSE, TRUE, UNKNOWN };
enum Operation {XOR, OR, AND};

static ostream &operator<<(ostream &os, Operation operation) {
  switch (operation) {
  case XOR:
    os << "XOR";
    break;
  case OR:
    os << "OR";
    break;
  case AND:
    os << "AND";
    break;
  }
  return os;
}

struct Gate;

struct Wire {
  string m_id;
  WireState m_wireState;
  set<Gate *> m_inputGates;

  Wire(string const &id) : m_id(id), m_wireState(UNKNOWN) {}
  Wire(string const &id, bool state)
      : m_id(id), m_wireState(state ? TRUE : FALSE) {}


  bool operator<(Wire const &p) const {
    return m_id < p.m_id;
  }
};

struct WireStore {
  vector<Wire> m_wires;
  map<string, size_t> m_wireIndex;

  void addWire(string const& id, string const& state) {
    m_wireIndex[id] = m_wires.size();
    m_wires.emplace_back(id, state == "1" ? TRUE : FALSE);
  }

  size_t addWire(string const& id, Gate* inputGate) {
    m_wireIndex[id] = m_wires.size();
    m_wires.emplace_back(id);
    m_wires.back().m_inputGates.insert(inputGate);
    return m_wires.size() - 1;
  }

  size_t countWires(char prefix) const {
    size_t count(0);
    for (auto &it : m_wireIndex) {
      if (it.first[0] == prefix) {
        ++count;
      }
    }
    return count;
  }

  size_t getIndex(const string &id) const {
      return m_wireIndex.find(id)->second;
  }

  Wire &getWire(const string &id) {
      return m_wires[getIndex(id)];
  }

  string wireId(size_t index) const {
      return m_wires[index].m_id;
  }

  void setState(size_t index, bool state);
};

struct Gate {
  size_t m_input1;
  size_t m_input2;
  Operation m_operation;
  size_t m_output;
  bool m_triggered;

  Gate(const string& input1, const string& operation, const string& input2, const string& output, WireStore& wires) 
      : m_triggered(false) {
    if (operation == "XOR") {
      m_operation = XOR;
    } else if (operation == "OR") {
      m_operation = OR;
    } else if (operation == "AND") {
      m_operation = AND;
    } else {
      cout << "Oops... " << operation << endl;
      exit(666);
    }

    m_input1 = link(input1, wires);
    m_input2 = link(input2, wires);
    m_output = link(output, wires);
  }

  void trigger(WireStore &wires) {
    if (!m_triggered) {
      WireState state1(wires.m_wires[m_input1].m_wireState);
      WireState state2(wires.m_wires[m_input2].m_wireState);

      if (state1 == UNKNOWN || state2 == UNKNOWN) {
        return;
      }

      m_triggered = true;
      switch (m_operation) {
      case OR:
        wires.setState(m_output, state1 == TRUE || state2 == TRUE);
        break;
      case XOR:
        wires.setState(m_output, (state1 == TRUE) ^ (state2 == TRUE));
        break;
      case AND:
        wires.setState(m_output, (state1 == TRUE) && (state2 == TRUE));
        break;
      }
    }
  }

  size_t link(string const &key, WireStore &wires) {
    auto fnd(wires.m_wireIndex.find(key));
      
    if (fnd != wires.m_wireIndex.end()) {
      wires.m_wires[fnd->second].m_inputGates.insert(this);
      return fnd->second;
    }

    return wires.addWire(key, this);
  }

  string operation() {
    switch (m_operation) {
    case OR:
      return "OR";
    case AND:
      return "AND";
    case XOR:
      return "XOR";
    }
    return "???";
  }

  void log(const WireStore &wireStore) const {
    cout << wireStore.wireId(m_input1) << ' ' << m_operation << ' '
         << wireStore.wireId(m_input2) << " -> " << wireStore.wireId(m_output)
         << endl;
  }
};

void WireStore::setState(size_t index, bool state) {
  if (m_wires[index].m_wireState == UNKNOWN) {
    m_wires[index].m_wireState = state ? TRUE : FALSE;
    for (Gate *gate : m_wires[index].m_inputGates) {
      gate->trigger(*this);
    }
  }
}

struct Calculator {
  WireStore m_wireStore;
  vector<Gate> m_gates;

  Calculator(const vector<string> &data) {
    bool gatesSized(false);
    for (const string &l : data) {
      if (l.empty()) {
        continue;
      }
      regex wirePattern{"([a-z0-9]{3}): (0|1)"};
      smatch wireMatch;

      if (regex_search(l, wireMatch, wirePattern)) {
        m_wireStore.addWire(wireMatch[1].str(), wireMatch[2].str());
      } else {
        regex pattern{
            "([a-z0-9]{3}) (XOR|OR|AND) ([a-z0-9]{3}) -> ([a-z0-9]{3})"};
        smatch match;

        if (regex_search(l, match, pattern)) {
          if (!gatesSized) {
            m_gates.reserve(data.size() - m_wireStore.m_wires.size());
            gatesSized = true;
          }
          m_gates.emplace_back(match[1].str(), match[2].str(), match[3].str(),
                               match[4].str(), m_wireStore);
        }
      }
    }
  }

  void compute() {
    for (;;) {
      for (auto &it : m_gates) {
        it.trigger(m_wireStore);
      }
      bool again(false);
      for (const auto &it : m_wireStore.m_wires) {
        if (it.m_wireState == UNKNOWN) {
          again = true;
          break;
        }
      }
      if (!again) {
        break;
      }
    }
  }

  string makeWireId(char side, int bit) const {
    string key;
    key += side;
    if (bit < 10) {
      key += '0';
    } else {
      key += '0' + bit / 10;
    }
    key += '0' + (bit % 10);
    return key;
  }

  solveResult getResult() const {
    solveResult r(0);

    for (size_t i(0); i < m_wireStore.m_wires.size(); ++i) {
      if (m_wireStore.m_wires[i].m_wireState == UNKNOWN) {
        exit(667);
      }
      if (m_wireStore.m_wires[i].m_id[0] == 'z' &&
          m_wireStore.m_wires[i].m_wireState == TRUE) {
        solveResult bit(stoll(m_wireStore.m_wires[i].m_id.substr(1)));
        r += 1LL << bit;
      }
    }

    return r;
  }

  Gate* findGate(Wire& a, Wire& b, Operation operation) {
    for (Gate *gate : a.m_inputGates) {
      if (gate->m_operation == operation) {
        for (Gate *gate2 : b.m_inputGates) {
          if (gate->m_output == gate2->m_output) {
            return gate;
          }
        }
      }
    }
    return NULL;
  }

  Gate *findGate(Wire &a, Operation operation) {
    for (Gate *gate : a.m_inputGates) {
      if (gate->m_operation == operation) {
        return gate;
      }
    }
    return NULL;
  }

  string checkBinaryAdder(int bit, const string& carryWire, set<string> &errorWires) {
    Wire &x(m_wireStore.getWire(makeWireId('x', bit)));
    Wire &y(m_wireStore.getWire(makeWireId('y', bit)));
    size_t z(m_wireStore.getIndex(makeWireId('z', bit)));

    // add is XOR of the 2 wires
    Gate *initialXor(findGate(x, y, XOR));

    if (initialXor != NULL) {
      if (bit == 0 || carryWire.empty()) {
        // no carry, direct to output
        if (initialXor->m_output != z) {
          errorWires.insert(m_wireStore.m_wires[initialXor->m_output].m_id);
        }
      } else {
        // with carry, need to xor that too.
        Gate *finalXor(findGate(m_wireStore.m_wires[initialXor->m_output],
                                m_wireStore.getWire(carryWire), XOR));

        if (finalXor != NULL && finalXor->m_output != z) {
          errorWires.insert(m_wireStore.m_wires[finalXor->m_output].m_id);
          errorWires.insert(makeWireId('z', bit));
          // now repair...
          for (Gate &gate : m_gates) {
            if (gate.m_output == z) {
              gate.m_output = finalXor->m_output;
              break;
            }
          }
          finalXor->m_output = z;
          cout << "repaired" << endl;
        } else if (finalXor == NULL) {
          // Rule the output wire of the initial xor must be an input into the final xor
          Gate *xOrCarry(findGate(m_wireStore.getWire(carryWire), XOR));
          if (xOrCarry != NULL && xOrCarry->m_output == z) {
            size_t xorCarryInput;
            size_t badWire(initialXor->m_output); // cvp
            if (xOrCarry->m_input1 == m_wireStore.getIndex(carryWire)) {
                xorCarryInput = xOrCarry->m_input2;
            } else {
              xorCarryInput = xOrCarry->m_input1;
            }
            for (Gate &gate : m_gates) {
              if (gate.m_output == xorCarryInput) {
                gate.m_output = badWire;
                gate.log(m_wireStore);
              }
            }
            initialXor->m_output = xorCarryInput;
            initialXor->log(m_wireStore);
            errorWires.insert(m_wireStore.wireId(badWire));
            errorWires.insert(m_wireStore.wireId(xorCarryInput));
            finalXor = findGate(m_wireStore.m_wires[initialXor->m_output], m_wireStore.getWire(carryWire), XOR);
            cout << "carry OK, repaired" << endl;
          }
        }
      }

      const Gate *bothBits(findGate(x, y, AND));
      // Locate and return the carry wire.
      if (bit == 0 || carryWire.empty()) {
        if (carryWire.empty() && bit > 0) {
          cout << "Bad carry state" << endl;
        }
        return m_wireStore.m_wires[bothBits->m_output].m_id;
      }
      const Gate *carryAndBit(findGate(m_wireStore.m_wires[initialXor->m_output], m_wireStore.getWire(carryWire), AND));

      if (carryAndBit != NULL && bothBits != NULL) {
        const Gate *combinedOr(findGate(m_wireStore.m_wires[carryAndBit->m_output],  m_wireStore.m_wires[bothBits->m_output], OR));
        if (combinedOr != NULL) {
          return m_wireStore.m_wires[combinedOr->m_output].m_id;
        }
      }
      if (carryAndBit == NULL) {
        cout << "No carry AND" << endl;
      }
      if (bothBits == NULL) {
        cout << "missing inputs AND" << endl;
      }
    } else {
      cout << "missing input " << bit << endl;
    }

    return "";
  }
};

day24Solver::day24Solver(const string &testFile) : solver(testFile) {}

void day24Solver::loadData(const string &line) {
  if (!line.empty()) {
    m_data.push_back(line);
  }
}

void day24Solver::clearData() {
    m_data.clear(); 
}

solveResult day24Solver::compute() {
  Calculator calculator(m_data);

  calculator.compute();

  return calculator.getResult();
}

string day24Solver::computeString() {
  if (m_test) {
    return ""; 
  }
  Calculator calculator(m_data);

  size_t srcCount(calculator.m_wireStore.countWires('x'));
  set<string> errorWires;
  string carry("");
  for (size_t srcIndex(0); srcIndex < srcCount; ++srcIndex) {
    carry = calculator.checkBinaryAdder(srcIndex, carry, errorWires);
  }

  string r;
  for (const string &e : errorWires) {
    if (!r.empty()) {
      r += ',';
    }
    r += e;
  }

   return r;
}

void day24Solver::loadTestData() {
  m_test = true;

  clearData();

  loadData("x00: 1");
  loadData("x01: 0");
  loadData("x02: 1");
  loadData("x03: 1");
  loadData("x04: 0");
  loadData("y00: 1");
  loadData("y01: 1");
  loadData("y02: 1");
  loadData("y03: 1");
  loadData("y04: 1");

  loadData("ntg XOR fgs -> mjb");
  loadData("y02 OR x01 -> tnw");
  loadData("kwq OR kpj -> z05");
  loadData("x00 OR x03 -> fst");
  loadData("tgd XOR rvg -> z01");
  loadData("vdt OR tnw -> bfw");
  loadData("bfw AND frj -> z10");
  loadData("ffh OR nrd -> bqk");
  loadData("y00 AND y03 -> djm");
  loadData("y03 OR y00 -> psh");
  loadData("bqk OR frj -> z08");
  loadData("tnw OR fst -> frj");
  loadData("gnj AND tgd -> z11");
  loadData("bfw XOR mjb -> z00");
  loadData("x03 OR x00 -> vdt");
  loadData("gnj AND wpb -> z02");
  loadData("x04 AND y00 -> kjc");
  loadData("djm OR pbm -> qhw");
  loadData("nrd AND vdt -> hwm");
  loadData("kjc AND fst -> rvg");
  loadData("y04 OR y02 -> fgs");
  loadData("y01 AND x02 -> pbm");
  loadData("ntg OR kjc -> kwq");
  loadData("psh XOR fgs -> tgd");
  loadData("qhw XOR tgd -> z09");
  loadData("pbm OR djm -> kpj");
  loadData("x03 XOR y03 -> ffh");
  loadData("x00 XOR y04 -> ntg");
  loadData("bfw OR bqk -> z06");
  loadData("nrd XOR fgs -> wpb");
  loadData("frj XOR qhw -> z04");
  loadData("bqk OR frj -> z07");
  loadData("y03 OR x01 -> nrd");
  loadData("hwm AND bqk -> z03");
  loadData("tgd XOR rvg -> z12");
  loadData("tnw OR pbm -> gnj");
}