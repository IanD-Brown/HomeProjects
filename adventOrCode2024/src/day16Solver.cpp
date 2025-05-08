#include "day16Solver.h"

#include <climits>
#include <iomanip>
#include <iostream>
#include <functional>
#include <map>
#include <sstream>

using namespace std;

static const char WALL('#');

enum Direction { NORTH, SOUTH, EAST, WEST };

static const Direction ALL_DIRECTIONS[] = {NORTH, SOUTH, EAST, WEST};

static const char *ToString(const Direction &v) {
	switch(v) {
	case NORTH:
		return "N";
	case SOUTH:
		return "S";
	case EAST:
		return "E";
	case WEST:
		return "W";
	}
	return NULL;
}

static Direction opposite(const Direction &from) {
	switch (from) {
	case NORTH:
		return SOUTH;
	case SOUTH:
		return NORTH;
	case EAST:
		return WEST;
	case WEST:
		return EAST;
	}
}

static coordinate move(const coordinate &from, const Direction &direction) {
	switch(direction) {
	case NORTH:
		return make_pair(COORD_ROW(from) - 1,COORD_COL(from));
	case SOUTH:
		return make_pair(COORD_ROW(from) + 1,COORD_COL(from));
	case EAST:
		return make_pair(COORD_ROW(from),COORD_COL(from) - 1);
	case WEST:
		return make_pair(COORD_ROW(from),COORD_COL(from) + 1);
	}
	return make_pair(0,0);
}

struct CellCost {
	bool m_endCell;
	map<Direction, solveResult> m_baseCosts;
	bool m_inBestPath;

	CellCost() : m_endCell(false), m_inBestPath(false) {}
	
	bool adjustBaseCost(solveResult cost, Direction direction) {
		if (m_endCell || cost == LLONG_MAX) {
			return false;
		}
		map < Direction, solveResult>::const_iterator opposing(m_baseCosts.find(opposite(direction)));
		if (opposing != m_baseCosts.cend()) {
			if (opposing->second < cost) {
				return false;
			} else {
				m_baseCosts.erase(opposing->first);
			}
		}
		map<Direction, solveResult>::const_iterator fnd(m_baseCosts.find(direction));

		if (fnd == m_baseCosts.cend() || fnd->second > cost) {
			m_baseCosts[direction] = cost;
			return true;
		}
		return false;
	}

	set<Direction> getDirections() const { 
		set<Direction> r;
		for (const auto c : m_baseCosts) {
			r.insert(c.first);
		}
		return r;
	}

	solveResult getCost(const Direction &entry) const {
		if (m_endCell) {
			return 1;
		}
		map<Direction, solveResult>::const_iterator fnd(m_baseCosts.find(entry));

		if (fnd != m_baseCosts.cend()) {
			return fnd->second + 1LL;
		}
		solveResult cost(LLONG_MAX);
		switch (entry) {
		case NORTH: 
		case SOUTH:
			for (const Direction turn : {EAST, WEST}) {
				fnd = m_baseCosts.find(turn);
				if (fnd != m_baseCosts.cend()) {
					cost = min(cost, fnd->second + 1001LL);
				}
			}
			break;
		case EAST:
		case WEST:
			for (const Direction turn : {NORTH, SOUTH}) {
				fnd = m_baseCosts.find(turn);
				if (fnd != m_baseCosts.cend()) {
					cost = min(cost, fnd->second + 1001LL);
				}
			}
			break;
		}
		return cost;
	}

	static friend ostream &operator<<(ostream &os, CellCost const &cellCost) {
		if (cellCost.m_endCell) {
			os << "end cell";
		} else {
			for (const auto c : cellCost.m_baseCosts) {
				os << c.second << " exiting " << ToString(c.first) << ',';
			}
		}
		
		return os;
	}
};

struct PathFinder {
	const coordinate &m_start;
	const coordinate &m_end;
	map<coordinate, CellCost> m_cellCosts;
	const vector<string> &m_data;
	vector<coordinate> path;
	int pathCount;
	PathFinder(const day16Solver &problem) :
		m_start(problem.m_start), m_end(problem.m_end), m_data(problem.m_data), pathCount(0) {
		for (size_t r = 0; r < problem.m_data.size(); ++r) {
			const string &line(problem.m_data[r]);
			for (size_t c = 0; c < line.size(); ++c) {
				if (line[c] != WALL) {
					m_cellCosts[make_pair(r, c)] = {};
				}
			}
		}
		m_cellCosts.find(m_end)->second.m_endCell = true;
	}

	bool isPath(const coordinate &from, const Direction &move) const {
		switch (move) {
		case NORTH:
			return m_data[COORD_ROW(from) - 1][COORD_COL(from)] != WALL;
		case SOUTH:
			return m_data[COORD_ROW(from) + 1][COORD_COL(from)] != WALL;
		case EAST:
			return m_data[COORD_ROW(from)][COORD_COL(from) - 1] != WALL;
		case WEST:
			return m_data[COORD_ROW(from)][COORD_COL(from) + 1] != WALL;
		}
		return false;
	}

	solveResult calc() {
		set<coordinate> processing({m_end});

		for (; !processing.empty();) {
			set<coordinate> followers;
			for (const auto &p : processing) {
				calc(p, [&](coordinate c) { followers.insert(c); });
			}
			processing.clear();
			processing.insert(followers.begin(), followers.end());
		}
		logCosts();
		const auto &startCost(m_cellCosts[m_start]);
		solveResult r(LLONG_MAX);
		for (const auto& c : startCost.m_baseCosts) {
			if (c.first == EAST) {
				return c.second;
			}
			r = min(r, c.second);
		}
		return r + 1000LL;
	}

	void calc(const coordinate &from, function<void(coordinate)> follow);

	solveResult countCellsInBestPath();

	solveResult markBestPathCells(const coordinate &from, const set<Direction> &directions);

	void logCosts() const;
};

void PathFinder::calc(const coordinate &from, function<void(coordinate)> follow) {
	const map<coordinate, CellCost>::iterator fromCost(m_cellCosts.find(from));

	for (const auto &fromEntry : ALL_DIRECTIONS) {
		if (isPath(from, fromEntry)) {
			coordinate entryCell(move(from, fromEntry));
			const map<coordinate, CellCost>::iterator entryCellCost(m_cellCosts.find(entryCell));

			if (entryCellCost->second.adjustBaseCost(fromCost->second.getCost(fromEntry), fromEntry)) {
				follow(entryCell);
			}
		}
	}
}

solveResult PathFinder::countCellsInBestPath() {
	const map<coordinate, CellCost>::iterator startCost(m_cellCosts.find(m_start));
	startCost->second.m_inBestPath = true;
	return markBestPathCells(m_start, startCost->second.getDirections());
}

solveResult PathFinder::markBestPathCells(const coordinate &from, const set<Direction> &directions) {
	solveResult result(0);

	for (const Direction &direction : directions) {
		coordinate next(move(from, opposite(direction)));
		const map<coordinate, CellCost>::iterator nextCost(m_cellCosts.find(next));
		path.push_back(next);

		if (!nextCost->second.m_inBestPath) {
			nextCost->second.m_inBestPath = true;
			result += 1 + markBestPathCells(next, nextCost->second.getDirections());
		}
		path.pop_back();
	}

	if (from == m_start) {
		//for (const auto &c : m_cellCosts) {
		//	if (c.second.m_inBestPath) {
		//		cout << COORD_ROW(c.first) << '/' << COORD_COL(c.first);
		//		if (c.second.m_endCell) {
		//			cout << "(0),";
		//		} else {
		//			cout << '(';
		//			for (map<Direction, solveResult>::const_iterator d =
		//					 c.second.m_baseCosts.cbegin();
		//				 d != c.second.m_baseCosts.cend(); ++d) {
		//				if (d != c.second.m_baseCosts.cbegin()) {
		//					cout << ';';
		//				}
		//				cout << ToString(d->first) << ':' << d->second;
		//			}
		//			cout << ')';
		//		}

		//		cout << endl;
		//	}
		//}
		//vector<string> now(m_data);
		//solveResult count(0);
		//for (const auto f : m_cellCosts) {
		//	if (f.second.m_inBestPath) {
		//		now[COORD_ROW(f.first)][COORD_COL(f.first)] = '0';
		//		++count;
		//	}
		//}
		//for (const auto l : now) {
		//	cout << l << endl;
		//}
		//cout << count << endl;
	}
	if (directions.empty() && from == m_end) {
		cout << "Path " << pathCount << endl;
		for (const auto &c : path) {
			cout << c.first << '/' << c.second << ' ';
			cout << '(';
			const auto &cc(m_cellCosts.find(c));
			for (map<Direction, solveResult>::const_iterator d =
					 cc->second.m_baseCosts.cbegin();
				 d != cc->second.m_baseCosts.cend(); ++d) {
				if (d != cc->second.m_baseCosts.cbegin()) {
					cout << ';';
				}
				cout << ToString(d->first) << ':' << d->second;
			}
			cout << ')' << endl;
		}
		cout << "End " << pathCount++ << ' ' << path.size() << endl;
	}
	return result;
}

void PathFinder::logCosts() const {
	int r(0);
	int c(0);
	for (const auto f : m_cellCosts) {
		int cellRow(COORD_ROW(f.first));
		if (cellRow > r) {
			cout << endl;
			r = cellRow;
			c = 0;
		}
		int cellCol(COORD_COL(f.first));
		for (; c + 1 < cellCol; ++c) {
			cout << "    ,";
		}
		c = cellCol;

		if (f.second.m_endCell) {
			cout << "    ,";
		} else {
			for (map<Direction, solveResult>::const_iterator c =
					 f.second.m_baseCosts.cbegin();
				 c != f.second.m_baseCosts.cend(); ++c) {
				if (c != f.second.m_baseCosts.cbegin()) {
					cout << ';';
				}
				cout << ToString(c->first) << ':' << c->second;
			}
			cout << ',';
		}
	}
	cout << endl;
}

day16Solver::day16Solver(const string &testFile) :
	solver(testFile) {}

void day16Solver::loadData(const string &line) { 
	size_t pos(line.find('S'));
	if (pos != string::npos) {
		m_start = make_pair(m_data.size(), pos);
	} else {
		pos = line.find('E');
		if (pos != string::npos) {
			m_end = make_pair(m_data.size(), pos);
		}
	}
	m_data.push_back(line);
}

void day16Solver::clearData() { 
	m_data.clear();
}

solveResult day16Solver::compute() {
	PathFinder pathFinder(*this);
	if (m_part1) {
		return pathFinder.calc();
	} else {
		pathFinder.calc();
		return pathFinder.countCellsInBestPath();
	}
}

void day16Solver::loadTestData() {
	clearData();

	loadData("#####");
	loadData("#..E#");
	loadData("#.###");
	loadData("#...#");
	loadData("###.#");
	loadData("#S..#");
	loadData("#####");

	assertEquals(compute(), m_part1 ? 4010 : 11, "Small one");

	loadData("#################");
	loadData("#...#...#...#..E#");
	loadData("#.#.#.#.#.#.#.#.#");
	loadData("#.#.#.#...#...#.#");
	loadData("#.#.#.#.###.#.#.#");
	loadData("#...#.#.#.....#.#");
	loadData("#.#.#.#.#.#####.#");
	loadData("#.#...#.#.#.....#");
	loadData("#.#.#####.#.###.#");
	loadData("#.#.#.......#...#");
	loadData("#.#.###.#####.###");
	loadData("#.#.#...#.....#.#");
	loadData("#.#.#.#####.###.#");
	loadData("#.#.#.........#.#");
	loadData("#.#.#.#########.#");
	loadData("#S#.............#");
	loadData("#################");

	assertEquals(compute(), m_part1 ? 11048 : 11, "Second case");

	loadData("###############");
	loadData("#.......#....E#");
	loadData("#.#.###.#.###.#");
	loadData("#.....#.#...#.#");
	loadData("#.###.#####.#.#");
	loadData("#.#.#.......#.#");
	loadData("#.#.#####.###.#");
	loadData("#...........#.#");
	loadData("###.#.#####.#.#");
	loadData("#...#.....#.#.#");
	loadData("#.#.#.###.#.#.#");
	loadData("#.....#...#.#.#");
	loadData("#.###.#.#.#.#.#");
	loadData("#S..#.....#...#");
	loadData("###############");
}
