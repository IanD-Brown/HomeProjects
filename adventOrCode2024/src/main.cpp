#include <cassert>
#include <typeinfo>
#include <iostream>
#include <functional>

#include "day1Solver.h"
#include "day2Solver.h"
#include "day3Solver.h"
#include "day4Solver.h"
#include "day5Solver.h"
#include "day6Solver.h"
#include "day7Solver.h"
#include "day8Solver.h"
#include "day9Solver.h"
#include "day10Solver.h"
#include "day11Solver.h"
#include "day12Solver.h"
#include "day13Solver.h"
#include "day14Solver.h"
#include "day15Solver.h"
#include "day16Solver.h"
#include "day17Solver.h"
#include "day18Solver.h"
#include "day19Solver.h"
#include "day20Solver.h"
#include "day21Solver.h"

using namespace std;

void invoke(solver* day, solveResult t, function<bool(solveResult)> pt1, solveResult t2, function<bool(solveResult) > pt2) {
	day->loadTestData();
	solveResult t1 = day->compute();
	cout << day->getDay() << " Test " << t1 << endl;
	assert(t1 == t && " wrong test value");

	day->loadFromFile();
	solveResult c = day->compute();
	std::cout << day->getDay() << " " << c << std::endl;
	assert(pt1(c) && " wrong day value");

	if ( t2 >= 0 ) {
		day->setPart2();
		day->loadTestData();
		solveResult x = day->compute2();
		cout << day->getDay() << " Test2 " << x << " vs " << t2 << endl;
		assert(x == t2 && " wrong test2 value");

		day->loadFromFile();
		solveResult c2 = day->compute2();
		std::cout << day->getDay() << "b " << c2 << std::endl;
		assert(pt2(c2) && " wrong day value b");
	}
}

void invoke(solver *day, string t, string pt1, string t2, string pt2) {
	day->loadTestData();
	string t1 = day->computeString();
	cout << day->getDay() << " Test " << t1 << endl;
	assert(t1 == t && " wrong test value");

	day->loadFromFile();
	string c = day->computeString();
	std::cout << day->getDay() << " " << c << std::endl;
	assert(pt1 == c && " wrong day value");

	day->setPart2();
	day->loadTestData();
	string x = day->computeString2();
	cout << day->getDay() << " Test2 " << x << endl;
	assert(x == t2 && " wrong test2 value");

	day->loadFromFile();
	string c2 = day->computeString2();
	std::cout << day->getDay() << "b " << c2 << std::endl;
	assert(pt2 == c2 && " wrong day value b");
}

struct lt {
	solveResult m_compare;
	lt(solveResult v) :
		m_compare(v) {}

	bool operator()(solveResult t) { return t < m_compare; }
};

struct gt {
	solveResult m_compare;
	gt(solveResult v) : m_compare(v) {
	}

	bool operator()(solveResult t) {
		return t > m_compare;
	}
};

 struct eq {
	solveResult m_compare;
	eq(solveResult v) : m_compare(v) {
	}

	bool operator()(solveResult t) {
		return t == m_compare;
	}
};

struct ne {
	 solveResult m_compare;
	 ne(solveResult v) :
		 m_compare(v) {}

	 bool operator()(solveResult t) { return t != m_compare; }
};


 struct between {
	 solveResult m_greater;
	 solveResult m_less;
	 between(solveResult v1, solveResult v2) : m_greater(v1), m_less(v2) {
	 }

	 bool operator()(solveResult t) {
		 return t > m_greater && t < m_less;
	 }
 };

int main() {
//    new day1Solver("day1\\data");
//    new day2Solver("day2\\data.txt");
//    new day3Solver("day3\\data.txt");
//    new day4Solver("day4\\data.txt");
//  new day5Solver("day5\\data.txt");
//  invoke(new day6Solver("day6\\data.txt"), 41, 5177, -1, 6, 1686, -1);
//  invoke(new day7Solver("day7\\data.txt"), 3749, -1, -1, 11387, 1, -1);

//	invoke(new day8Solver("day8\\data.txt"), 14, [ ] (long v) {return v == 311; }, 34, [ ] (long v2) {return v2 == 1115; });
//	invoke(new day9Solver("day9\\data.txt"), 1928, eq(6320029754031LL), 2858, eq(6347435485773LL));
//	invoke(new day10Solver("day10\\data.txt"), 36, eq(694), 81, eq(1497));
//	invoke(new day11Solver("day11\\data.txt"), 55312, eq(202019), 65601038650482LL, eq(239321955280205LL));
//	invoke(new day12Solver("day12\\data.txt"), 1930, eq(1461752), 1206, eq(904114LL));
//invoke(new day13Solver("day13\\data.txt"), 480, eq(26810), 875318608908LL, eq(108713182988244LL));
//invoke(new day14Solver("day14\\data.txt"), 12, eq(230436441LL), 0, eq(8270));
//invoke(new day15Solver("day15\\data.txt"), 10092, eq(1495147LL), 9021, eq(1524905LL));
//invoke(new day16Solver("day16\\data.txt"), 7036, eq(104516LL), 45, eq(545LL));
//invoke(new day17Solver("day17\\data.txt"), "4,6,3,5,6,3,5,2,1,0", "1,7,2,1,4,1,5,4,0", "117440", "37221261688308");
//invoke(new day18Solver("day18\\data.txt"), 22, eq(316LL), 13, eq(1323LL));
//invoke(new day19Solver("day19\\data.txt"), 6, eq(363LL), 16, eq(642535800868438LL));
//invoke(new day20Solver("day20\\data.txt"), 44, eq(1369LL), 
//	32 + 31 + 29 + 39 + 25 + 23 + 20 + 19 + 12 + 14 + 12 + 22 + 4 + 3,
//	   eq(979012LL));
invoke(new day19Solver("day21\\data.txt"), 126384, eq(363LL), 16, eq(642535800868438LL));

  return 0;
}