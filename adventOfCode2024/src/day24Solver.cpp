#include "day24Solver.h"

#include <bitset>
#include <cstdlib>
#include <iostream>
#include <map>
#include <regex>
#include <set>
#include <solver.h>
#include <string>
#include <vector>

using namespace std;

static Key toKey(const string &value, size_t limit) {
  Key key(0);

  for (size_t i(0); i < limit; ++i) {
    int v(value[i] >= '0' &&
          value[i] <= '9' ? value[i] - '0' : 10 + value[i] - 'a');
    key = (key << 8) | v;
  }

  return key;
}

static Key toKey(const string &value) { return toKey(value, value.size()); }

static string toString(Key key) { 
  string s;
  while (key > 0) {
    char v(key & 0xff);
    v += v < 10 ? '0' : 'a' - 10;
    s.insert(s.begin(), v);
    key >>= 8;
  }
  return s;
}

enum WireState { FALSE, TRUE, UNKNOWN };
enum Operation {XOR, OR, AND};

struct Gate;

struct Wire {
  WireState m_wireState;
  set<Gate *> m_inputGates;

  Wire() : m_wireState(UNKNOWN) {}
  Wire(bool state) : m_wireState(state ? TRUE : FALSE) {}

  void setState(bool state, map<Key, Wire> &wires);
};

struct Gate {
  Key m_input1;
  Key m_input2;
  Operation m_operation;
  Key m_output;
  bool m_triggered;

  Gate(const string& input1, const string& operation, const string& input2,
      const string& output, map<Key, Wire>& wires) 
      : m_input1(toKey(input1)), m_input2(toKey(input2)), m_output(toKey(output)), m_triggered(false) {
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

    link(m_input1, wires);
    link(m_input2, wires);
    link(m_output, wires);
  }

  void trigger(map<Key, Wire> &wires) {
    if (!m_triggered) {
      WireState state1(wires[m_input1].m_wireState);
      WireState state2(wires[m_input2].m_wireState);

      if (state1 == UNKNOWN || state2 == UNKNOWN) {
        return;
      }

      m_triggered = true;
      switch (m_operation) {
      case OR:
        wires[m_output].setState(state1 == TRUE || state2 == TRUE, wires);
        break;
      case XOR:
        wires[m_output].setState((state1 == TRUE) ^ (state2 == TRUE), wires);
        break;
      case AND:
        wires[m_output].setState((state1 == TRUE) && (state2 == TRUE), wires);
        break;
      }
    }
  }

  void link(Key key, map<Key, Wire> &wires) {
    auto fnd(wires.find(key));
      
    if (fnd != wires.end()) {
        fnd->second.m_inputGates.insert(this);
    } else {
      wires[key].m_inputGates.insert(this);
    }
  }
};

  void Wire::setState(bool state, map<Key, Wire> &wires) {
  if (m_wireState == UNKNOWN) {
    m_wireState = state ? TRUE : FALSE;
    for (Gate *gate : m_inputGates) {
      gate->trigger(wires);
    }
  }
}


day24Solver::day24Solver(const string &testFile) : solver(testFile) {}

void day24Solver::loadData(const string &line) { 
    m_data.push_back(line); 
}

void day24Solver::clearData() {
    m_data.clear(); 
}

solveResult day24Solver::compute() {
  solveResult r(0);
  map<Key, Wire> wires;
  vector<Gate> gates;

  for (const string &l : m_data) {
    if (l.empty()) {
      continue;
    }
    if (l[3] == ':') {
      wires.emplace(toKey(l, 3), l[5] == '1');
    } else {
      regex pattern{"([a-z0-9]{3}) (XOR|OR|AND) ([a-z0-9]{3}) -> ([a-z0-9]{3})"};
      smatch match;

      if (regex_search(l, match, pattern)) {
        gates.emplace_back(match[1].str(), match[2].str(), match[3].str(),
                      match[4].str(),
                      wires);
      }
    }
  }

  for (;;) {
    for (auto &it : gates) {
      it.trigger(wires);
    }
    bool again(false);
    for (const auto &it : wires) {
      if (it.second.m_wireState == UNKNOWN) {
        again = true;
        break;
      }
    }
    if (!again) {
      break;
    }
  }

  const Key firstResult(toKey("z00"));
  for (const auto &it : wires) {
    if (it.second.m_wireState == UNKNOWN) {
      exit(667);
    }
    if (it.first >= firstResult && it.second.m_wireState == TRUE) {
      solveResult bit(stoll(toString(it.first).substr(1)));
      r += 1LL << bit;
    }
  }

  return r;
}

string day24Solver::computeString() {
  string r;

  return r;
}

void day24Solver::loadTestData() {
  m_test = true;

  clearData();

  if (m_part1) {
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
  } else {
    loadData("x00: 0");
    loadData("x01: 1");
    loadData("x02: 0");
    loadData("x03: 1");
    loadData("x04: 0");
    loadData("x05: 1");
    loadData("y00: 0");
    loadData("y01: 0");
    loadData("y02: 1");
    loadData("y03: 1");
    loadData("y04: 0");
    loadData("y05: 1");
    
    loadData("x00 AND y00 -> z05");
    loadData("x01 AND y01 -> z02");
    loadData("x02 AND y02 -> z01");
    loadData("x03 AND y03 -> z03");
    loadData("x04 AND y04 -> z04");
    loadData("x05 AND y05 -> z00");
  }
}