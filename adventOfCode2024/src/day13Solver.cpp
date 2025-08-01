#include "day13Solver.h"

#include <iostream>
#include <regex>

using namespace std;

struct MachineBuilder {
	size_t m_aX;
	size_t m_aY;
	size_t m_bX;
	size_t m_bY;
	size_t m_prizeX;
	size_t m_prizeY;
	const regex m_buttonPattern;
	const regex m_prizePattern;
	
	MachineBuilder() 
		: m_buttonPattern("(Button )([AB])(: X\\+)(\\d+)(, Y\\+)(\\d+)"), 
		  m_prizePattern("(Prize: X=)(\\d+)(, Y=)(\\d+)") { 
		clear();
	}

	bool accept(const string &line) {
		smatch match;

		if (regex_match(line, match, m_buttonPattern)) {
			if (match[2] == "A") {
				m_aX = stoul(match[4]);
				m_aY = stoul(match[6]);
			} else {
				m_bX = stoul(match[4]);
				m_bY = stoul(match[6]);
			}
		} else if (regex_match(line, match, m_prizePattern)) {
			m_prizeX = stoul(match[2]);
			m_prizeY = stoul(match[4]);
			return true;
		}
		return false;
	}

	void build(vector<day13Solver::Machine> &destination) {
		destination.emplace_back(m_aX, m_aY, m_bX, m_bY, m_prizeX, m_prizeY);
		clear();
	}

	void clear() {
		m_prizeX = 0;
		m_prizeY = 0;
		m_aX = 0;
		m_aY = 0;
		m_bX = 0;
		m_bY = 0;
	}
};

static MachineBuilder s_machineBuilder;

day13Solver::day13Solver(const string& testFile) : solver(testFile) {
}

void day13Solver::loadData(const string &line) {
	if (s_machineBuilder.accept(line)) {
		s_machineBuilder.build(m_data);
	}
}

void day13Solver::clearData() { 
	m_data.clear(); 
}

solveResult day13Solver::compute() {
	solveResult t = 0;

	/*
	 * m_prizeX = n * m_aX + m * m_bX
	 * m_prizeY = n * m_aY + m * m_bY
	 * cost = 3 * n + m
	 * n and m 0..100
	 * can be no solution
	 * test figures
	 * a 80 b 40 machine aX 94 bX 22 aY 34 bY 67 prizeX 8400 prizeY 5400
     * a 38 b 86 machine aX 17 bX 84 aY 86 bY 37 prizeX 7870 prizeY 6450
	 * 
	 * so for the first case 
	 * 8400 = 80 * 94 + 40 * 22 = 7520 + 880
	 * 5400 = 80 * 34 + 40 * 67 = 2720 + 2680
	 * 
	 */
	for (const auto &machine : m_data) {
		solveResult calc = machine.cost(m_part1 ? 0 : 10000000000000LL);

		if (calc > 0) {
			t += calc;
		}
	}

	return t;
}

void day13Solver::loadTestData() {
	clearData();

	loadData("Button A: X+94, Y+34");
	loadData("Button B: X+22, Y+67");
	loadData("Prize: X=8400, Y=5400");

//	assertEquals(compute(), 280, "first machine");

	loadData("");
	loadData("Button A: X+26, Y+66");
	loadData("Button B: X+67, Y+21");
	loadData("Prize: X=12748, Y=12176");
//	assertEquals(compute(), 280, "second machine");

	loadData("");
	loadData("Button A: X+17, Y+86");
	loadData("Button B: X+84, Y+37");
	loadData("Prize: X=7870, Y=6450");
//	assertEquals(compute(), 480, "third machine");

	loadData("");
	loadData("Button A: X+69, Y+23");
	loadData("Button B: X+27, Y+71");
	loadData("Prize: X=18641, Y=10279");
}
