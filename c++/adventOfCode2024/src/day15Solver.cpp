#include "day15Solver.h"

#include <algorithm>
#include <cassert>
#include <iostream>
#include <fstream>
#include <sstream>

using namespace std;

static const char WALL('#');
static const char BOX('O');
static const char BOX_LEFT('[');
static const char BOX_RIGHT(']');
static const char EMPTY('.');

day15Solver::day15Solver(const string& testFile) : solver(testFile) {
}

void day15Solver::loadData(const string &line) {
	if (line.empty()) {
		return;
	}
	if (line[0] == WALL) {
		size_t pos = line.find('@');
		if (pos != string::npos) {
			m_position = make_pair(m_warehouse.size(), pos);
		}
		m_warehouse.push_back(line);
	} else {
		m_moves += line;
	}
}

void day15Solver::clearData() { 
	m_warehouse.clear();
	m_moves.clear();
}

size_t day15Solver::countBoxes() const { 
	size_t t(0);
	for (const auto& r : m_warehouse) {
		for (size_t i = 0; i < r.size(); ++i) {
			char c(r[i]);
			if (c == BOX || c == BOX_LEFT) {
				++t;
				if (c == BOX_LEFT && r[i + 1] != BOX_RIGHT) {
					cout << m_position.first << '/' << m_position.second
						 << endl;
					for (const auto &r : m_warehouse) {
						cout << r << endl;
					}
					assert(false);
				}
			}
		}
	}
	return t;
}

void day15Solver::transformWarehouse() {
	vector<string> transformed;
	for (const auto &l : m_warehouse) {
		stringstream ss;
		string s;
		for (const char c : l) {
			switch (c) { case WALL:
				ss << WALL << WALL;
				break;
			case BOX:
				ss << BOX_LEFT << BOX_RIGHT;
				break;
			case EMPTY:
			case '@':
				ss << "..";
				break;
			}
		}
		ss >> s;
		transformed.push_back(s);
	}
	m_warehouse.clear();
	m_warehouse.assign(transformed.begin(), transformed.end());
	m_position.second = m_position.second * 2;
}

void day15Solver::changeColumn(int adjust) {
	string &line(m_warehouse[COORD_ROW(m_position)]);
	size_t c(COORD_COL(m_position));
	for (;;) {
		c += adjust;
		char current(line[c]);

		if (current == WALL) {
			return;
		}
		if (isBox(current)) {
			continue;
		}
		// example cases
		// c = 3, m_position.second = 6.
		// Therefore adjust = -1, need to set line[3] = line[4], line[4] = line[5] and line[5] = '.'
		// c = 5, m_position.second = 6.  Therefore adjust = -1, nothing to move.
		// c = 8, m_position.second = 6.  
		// Therefore adjust = 1, need to set line[8] = line[7], line[7] = '.'
		while (c - adjust != COORD_COL(m_position)) {
			line[c] = line[c - adjust];
			c -= adjust;
		}
		line[c] = EMPTY;
		line[COORD_COL(m_position)] = EMPTY;
		m_position.second += adjust;
		return;
	}
}

void day15Solver::changeRow(int adjust) {
	size_t r(COORD_ROW(m_position));
	size_t c(COORD_COL(m_position));
	if (changeRow(adjust, r, {c})) {
		// then change the position
		m_warehouse[r][c] = EMPTY;

		m_position.first += adjust;
	}
}

/*
 * adjust, amount that the row is being changed by (either +1 or -1)
 * row, index moving from
 * firstCol and lastCol, range of indices in the row to check.
 * 
 * The edge case.  Adjust is -1
 * a) row = 7, first/last = 7.  Detects box at 7/8 in 6
 * b) row = 6, first = 7, last = 8. Needs to detect box at 8/9 in 5
 * c) row = 5, first = 8, last = 9. Needs to detect box at 8/9 in 4
 * d) row = 4, first = 9, last = 9. Needs to detect box at 7/8 in 3
 * e) row = 3, first = 7, last = 8.  Empty!!!
 */
bool day15Solver::changeRow(int adjust, size_t row, const set<size_t>& columns) {
	set<size_t> recurseColumns;
	string &line(m_warehouse[row + adjust]);
	for (auto c : columns) {
		switch (line[c]) {
		case WALL:
			return false;
		case BOX:
			recurseColumns.insert(c);
			break;
		case BOX_LEFT:
			recurseColumns.insert(c);
			recurseColumns.insert(c + 1);
			break;
		case BOX_RIGHT:
			recurseColumns.insert(c);
			recurseColumns.insert(c - 1);
			break;
		}
	}

	if (!recurseColumns.empty() && !changeRow(adjust, row + adjust, recurseColumns)) {
		return false;
	}
	string &source(m_warehouse[row]);
	for (const auto c : columns) {
		line[c] = source[c];
		source[c] = EMPTY;
	}

	return true;
}

bool day15Solver::isBox(char c) const {
	if (m_part1) {
		return c == BOX;
	}
	return c == BOX_LEFT || c == BOX_RIGHT; 
}

solveResult day15Solver::compute() {
	if (!m_part1) {
		transformWarehouse();
	}
	const size_t boxCount(countBoxes());
	size_t count(0);
	for (const char c : m_moves) {
		switch (c) { 
		case '<':
			changeColumn(-1);
			break;
		case '>':
			changeColumn(1);
			break;
		case 'v':
			changeRow(1);
			break;
		case '^':
			changeRow(-1);
			break;
		}
		if (countBoxes() != boxCount) {
			cout << c << '-' << COORD_ROW(m_position) << '/'  << COORD_COL(m_position) << endl;
			for (const auto &r : m_warehouse) {
				cout << r << endl;
			}
			assert(false);
		}
		if (!m_part1 && m_test) {
			string str(to_string(count++));
			auto new_str(string(4 - min(4, (int)str.length()), '0') + str);
			ofstream fout("tmp/filename" + new_str + ".txt");
			m_warehouse[COORD_ROW(m_position)][COORD_COL(m_position)] = '@';
			for (const auto &r : m_warehouse) {
				fout << r << endl;
			}
			fout << c << '-' << COORD_ROW(m_position) << '/' << COORD_COL(m_position) << endl;
		}
	}
	solveResult t(0);
	for (int r = 0; r < m_warehouse.size(); ++r) {
		for (int c = 0; c < m_warehouse[r].size(); ++c) {
			if (m_warehouse[r][c] == BOX) {
				t += 100 * r + c;
			} else if (m_warehouse[r][c] == BOX_LEFT) {
				t += 100 * r + c;
			}
		}
	}
	return t;
}

void day15Solver::loadTestData() {
	clearData();
	bool partState(m_part1);
	bool testState(m_test);

	//m_part1 = false;

	//loadData("#######");
	//loadData("#...#.#");
	//loadData("#.....#");
	//loadData("#..OO@#");
	//loadData("#..O..#");
	//loadData("#.....#");
	//loadData("#######");
	//loadData("");
	//loadData("<vv<<^^<<^^");
	//loadData("");

	m_part1 = false;
//	m_test = true;
	loadData("#######");
	loadData("#.....#");
	loadData("#.....#");
	loadData("#.@O..#");
	loadData("#..#O.#");
	loadData("#...O.#");
	loadData("#..O..#");
	loadData("#.....#");
	loadData("#######");
	loadData("");
	loadData(">><vvv>v>^^^");
	//        0123456789ab

	assertEquals(compute(), 1430, "edge case");
	clearData();
	m_part1 = partState;
	m_test = testState;

	loadData("##########"); // 0
	loadData("#..O..O.O#"); // 1
	loadData("#......O.#"); // 2
	loadData("#.OO..O.O#"); // 3
	loadData("#..O@..O.#"); // 4
	loadData("#O#..O...#"); // 5
	loadData("#O..O..O.#"); // 6
	loadData("#.OO.O.OO#"); // 7
	loadData("#....O...#"); // 8
	loadData("##########"); // 9
	loadData("");
	loadData("<vv>^<v^>v>^vv^v>v<>v^v<v<^vv<<<^><<><>>v<vvv<>^v^>^<<<><<v<<<v^vv^v>^");
	loadData("vvv<<^>^v^^><<>>><>^<<><^vv^^<>vvv<>><^^v>^>vv<>v<<<<v<^v>^<^^>>>^<v<v");
	loadData("><>vv>v^v^<>><>>>><^^>vv>v<^^^>>v^v^<^^>v^^>v^<^v>v<>>v^v^<v>v^^<^^vv<");
	loadData("<<v<^>>^^^^>>>v^<>vvv^><v<<<>^^^vv^<vvv>^>v<^^^^v<>^>vvvv><>>v^<<^^^^^");
	loadData("^><^><>>><>^^<<^^v>>><^<v>^<vv>>v>>>^v><>^v><<<<v>>v<v<v>vvv>^<><<>^><");
	loadData("^>><>^v<><^vvv<^^<><v<<<<<><^v<<<><<<^^<v<^^^><^>>^<v^><<<^>>^v<v^v<v^");
	loadData(">^>>^v>vv>^<<^v<>><<><<v<<v><>v<^vv<<<>^^v^>^^>>><<^v>>v^v><^^>>^<>vv^");
	loadData("<><^^>^^^<><vvvvv^v<v<<>^v<v>v<<^><<><<><<<^^<<<^<<>><<><^^^>^^<>^>v<>");
	loadData("^^>vv<^v^v<vv>^<><v<^v>^^^>>>^^vvv^>vvv<>>>^<^>>>>>^<<^v>^vvv<>^<><<v>");
	loadData("v^^>>><<^^<>>^v^<v^vv<>v^<<>^<^v^v><^<<<><<^<v><v<>vv>>v><v^<vv<>v^<<^");
}
