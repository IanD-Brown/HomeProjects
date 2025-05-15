#include "day14Solver.h"

#include <iostream>
#include <regex>

using namespace std;

static solveResult s_width(101);
static solveResult s_height(103);
static solveResult moves(100);
static const regex s_robot("(p=)(\\d+)(,)(\\d+)( v=)([0-9-]+)(,)([0-9-]+)");

day14Solver::day14Solver(const string& testFile) : solver(testFile) {
}

void day14Solver::loadData(const string &line) {
	smatch match;
	if (regex_match(line, match, s_robot)) {
		m_data.emplace_back(make_pair(stoul(match[2]), stoul(match[4])), make_pair(stoll(match[6]), stoll(match[8])));
	}
}

void day14Solver::clearData() { 
	m_data.clear();
	s_width = 101;
	s_height = 103;
}

static size_t move(size_t start, solveResult perMove, solveResult moves, size_t limit) {
	solveResult result = start + perMove * moves;

	if (result < 0) {
		result += moves * limit;
	}
	return result % limit;
}

solveResult day14Solver::compute() {
	if (m_part1) {
		vector<coordinate> positions;

		for (const auto &r : m_data) {
			solveResult posX = move(r.m_startLocation.first, r.m_velocity.first,
									moves, s_width);
			solveResult posY = move(r.m_startLocation.second,
									r.m_velocity.second, moves, s_height);
			positions.emplace_back(posX, posY);
		}

		size_t midX = s_width / 2;
		size_t midY = s_height / 2;
		size_t tl(0);
		size_t tr(0);
		size_t bl(0);
		size_t br(0);

		for (const auto &p : positions) {
			if (p.first == midX || p.second == midY) {
				continue;
			}
			if (p.first < midX) {
				if (p.second < midY) {
					++tl;
				} else {
					++tr;
				}
			} else {
				if (p.second < midY) {
					++bl;
				} else {
					++br;
				}
			}
		}

		return tl * tr * bl * br;
	} else {
		if (m_data.size() > 12) {
			set<coordinate> positions;
			size_t moveCount(1);
			for (;; ++moveCount) {
				positions.clear();
				for (const auto &r : m_data) {
					auto result = positions.insert(make_pair(
						move(r.m_startLocation.first, r.m_velocity.first, moveCount, s_width),
						move(r.m_startLocation.second, r.m_velocity.second, moveCount, s_height)));
					if (!result.second) {
						break;
					}
				}
				if (positions.size() == m_data.size()) {
					return moveCount;
				}
			}
		}

		return 0;
	}
}

void day14Solver::loadTestData() {
	clearData();
	s_width = 11;
	s_height = 7;

	loadData("p=0,4 v=3,-3");
	loadData("p=6,3 v=-1,-3");
	loadData("p=10,3 v=-1,2");
	loadData("p=2,0 v=2,-1");
	loadData("p=0,0 v=1,3");
	loadData("p=3,0 v=-2,-2");
	loadData("p=7,6 v=-1,-3");
	loadData("p=3,0 v=-1,-2");
	loadData("p=9,3 v=2,3");
	loadData("p=7,3 v=-1,2");
	loadData("p=2,4 v=2,-3");
	loadData("p=9,5 v=-3,-3");
}
